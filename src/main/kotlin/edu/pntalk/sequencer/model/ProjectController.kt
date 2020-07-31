/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import edu.pntalk.sequencer.view.NewDirFragment
import edu.pntalk.sequencer.view.NewFileFragment
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTreeCell
import javafx.scene.input.Clipboard
import javafx.scene.paint.Color
import javafx.util.StringConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import tornadofx.*
import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * Controller for managing opened directory,
 * searching through files etc.
 */
class ProjectController: Controller() {
    private var project: ProjectFile = ProjectFile(createTempDir().absolutePath)
    val code: CodeController by inject()
    val alert: AlertController by inject()
    val treeview : TreeView<ProjectFile> = TreeView()
    val listTree : MutableList<TreeItem<ProjectFile>> = mutableListOf()

    init {
        treeview.maxWidth = 150.0
        val f = File(project.toPath().toString() + "\\HelloWorld.pntalk")
        if (f.exists())
            f.delete()

        f.writeText(generateWelcomeCode())
        alert.outputMessage("New project template generated in temp dir: ${project.toPath()}")

        GlobalScope.async {
            asWatchChannel().consumeEach { event ->
                print("Changed: ${event.file.absolutePath}")
                if (event.kind.kind == "created")
                    Platform.runLater {
                        updateTreeView()
                    }
            }
        }
    }

    class ProjectCell(val project: ProjectController) : TextFieldTreeCell<ProjectFile>() {

        val renameAction = MenuItem("Rename File..")
        val deleteAction = MenuItem("Delete File..")
        val copyPathAction = MenuItem("Copy Absolute Path..")

        val renameFolderAction = MenuItem("Rename Directory..")
        val createFolderAction = MenuItem("Create SubDirectory")
        val createAction = MenuItem("Create PNTalk File..")
        val createScrioAction = MenuItem("Create Scenario File..")
        val deleteFolderAction = MenuItem("Delete Directory Recursively..")

        class ProjectConverter(val cell: ProjectCell) : StringConverter<ProjectFile>() {
            override fun toString(project: ProjectFile?): String {
                return project?.name.orEmpty()
            }

            override fun fromString(path: String?): ProjectFile? {
                val newPath = cell.item.parentFile.absolutePath + File.separator + path
                val source = File(cell.item.absolutePath)
                val file = File(cell.item.parentFile.absolutePath + File.separator + path)
                source.renameTo(file)
                return path?.let { ProjectFile(newPath) }
            }
        }

        init {
            if (this != null)
            {
                converter = ProjectConverter(this)
            }
            renameAction.onAction = EventHandler{
                startEdit()
            }

            renameFolderAction.onAction = EventHandler{
                startEdit()
            }

            createAction.onAction = EventHandler {
                item?.let {
                    NewFileFragment(item.absolutePath + File.separator, ".pntalk", project).openWindow()
                }
            }

            createFolderAction.onAction = EventHandler {
                item?.let {
                    NewDirFragment(item.absolutePath + File.separator, project).openWindow()
                }
            }

            deleteAction.onAction = EventHandler {
                item?.let {
                    Files.deleteIfExists(item.toPath())
                    project.alert.infoMessage("File ${item.absolutePath} deleted.")
                    project.updateTreeView()
                }
            }

            copyPathAction.onAction = EventHandler {
                item?.let {
                    val clipboard = Clipboard.getSystemClipboard()
                    clipboard.putString(item.absolutePath)
                    project.alert.infoMessage("Copied into clipboard: ${item.absolutePath}")
                }
            }
        }

        override fun updateItem(item: ProjectFile?, empty: Boolean) {

            super.updateItem(item, empty)
            contextMenu = ContextMenu()
            contextMenu.items.clear()

            //contextMenu.items.clear()
            if (item != null) {
                if (item.isFile)
                    contextMenu.items.addAll(renameAction, deleteAction, copyPathAction)
                else
                    contextMenu.items.addAll(createAction, createScrioAction, createFolderAction, renameAction, deleteFolderAction)
            }
        }

        //override fun string

        override fun toString(): String {
            return "abc"
        }

        override fun commitEdit(newValue: ProjectFile?) {
            //val source = File(item.absolutePath)
            //super.commitEdit(newValue)
            //source.renameTo(newValue)
            project.updateTreeView()
        }

        /*fun createTextField() {
            val textField = TextField(getString())
            textField.onKeyReleased = EventHandler<KeyEvent> {

                if (it.code == KeyCode.ENTER)
                {
                    commitEdit(item)
                }
                else if (it.code == KeyCode.ESCAPE) {
                    cancelEdit()
                }
            }
        }*/

        /*fun getString() : String {
            return if (item != null) item.name else ""
        }*/
    }

    class ProjectFile : File {
        constructor(path: String) : super(path) {
            dirty = if (isDirectory) SimpleStringProperty("")
            else SimpleStringProperty(readText())
        }

        override fun toString(): String {
            return name
        }

        /**
         * Watches directory. If file is supplied it will use parent directory. If it's an intent to watch just file,
         * developers must filter for the file related events themselves.
         *
         * @param [mode] - mode in which we should observe changes, can be SingleFile, SingleDirectory, Recursive
         * @param [tag] - any kind of data that should be associated with this channel
         * @param [scope] - coroutine context for the channel, optional
         */
        fun File.asWatchChannel(
                mode: KWatchChannel.Mode? = null,
                tag: Any? = null,
                scope: CoroutineScope = GlobalScope
        ) = KWatchChannel(
                file = this,
                mode = mode ?: if (isFile) KWatchChannel.Mode.SingleFile else KWatchChannel.Mode.Recursive,
                scope = scope,
                tag = tag
        )


        /**
         * Wrapper around [WatchEvent] that comes with properly resolved absolute path
         */
        data class KWatchEvent(
                /**
                 * Abolute path of modified folder/file
                 */
                val file: File,

                /**
                 * Kind of file system event
                 */
                val kind: Kind,

                /**
                 * Optional extra data that should be associated with this event
                 */
                val tag: Any?
        ) {
            /**
             * File system event, wrapper around [WatchEvent.Kind]
             */
            enum class Kind(val kind: String) {
                /**
                 * Triggered upon initialization of the channel
                 */
                Initialized("initialized"),

                /**
                 * Triggered when file or directory is created
                 */
                Created("created"),

                /**
                 * Triggered when file or directory is modified
                 */
                Modified("modified"),

                /**
                 * Triggered when file or directory is deleted
                 */
                Deleted("deleted")
            }
        }

        class KWatchChannel(
                val file: File,
                val scope: CoroutineScope = GlobalScope,
                val mode: Mode,
                val tag: Any? = null,
                private val channel: Channel<KWatchEvent> = Channel()
        ) : Channel<KWatchEvent> by channel {

            private val watchService: WatchService = FileSystems.getDefault().newWatchService()
            private val registeredKeys = ArrayList<WatchKey>()
            private val path: Path = if (file.isFile) {
                file.parentFile
            } else {
                file
            }.toPath()

            private fun registerPaths() {
                registeredKeys.apply {
                    forEach { it.cancel() }
                    clear()
                }
                if (mode == Mode.Recursive) {
                    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                        override fun preVisitDirectory(subPath: Path, attrs: BasicFileAttributes): FileVisitResult {
                            registeredKeys += subPath.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
                            return FileVisitResult.CONTINUE
                        }
                    })
                } else {
                    registeredKeys += path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
                }
            }

            init {
                // commence emitting events from channel
                scope.launch(Dispatchers.IO) {

                    // sending channel initalization event
                    channel.send(
                            KWatchEvent(
                                    file = path.toFile(),
                                    tag = tag,
                                    kind = KWatchEvent.Kind.Initialized
                            ))

                    var shouldRegisterPath = true

                    while (!isClosedForSend) {

                        if (shouldRegisterPath) {
                            registerPaths()
                            shouldRegisterPath = false
                        }

                        val monitorKey = watchService.take()
                        val dirPath = monitorKey.watchable() as? Path ?: break
                        monitorKey.pollEvents().forEach {
                            val eventPath = dirPath.resolve(it.context() as Path)

                            if (mode == Mode.SingleFile && eventPath.toFile().absolutePath != file.absolutePath) {
                                return@forEach
                            }

                            val eventType = when(it.kind()) {
                                ENTRY_CREATE -> KWatchEvent.Kind.Created
                                ENTRY_DELETE -> KWatchEvent.Kind.Deleted
                                else -> KWatchEvent.Kind.Modified
                            }

                            val event = KWatchEvent(
                                    file = eventPath.toFile(),
                                    tag = tag,
                                    kind = eventType
                            )

                            // if any folder is created or deleted... and we are supposed
                            // to watch subtree we re-register the whole tree
                            if (mode == Mode.Recursive &&
                                    event.kind in listOf(KWatchEvent.Kind.Created, KWatchEvent.Kind.Deleted) &&
                                    event.file.isDirectory) {
                                shouldRegisterPath = true
                            }

                            channel.send(event)
                        }

                        if (!monitorKey.reset()) {
                            monitorKey.cancel()
                            close()
                            break
                        }
                        else if (isClosedForSend) {
                            break
                        }
                    }
                }
            }

            override fun close(cause: Throwable?): Boolean {
                registeredKeys.apply {
                    forEach { it.cancel() }
                    clear()
                }

                return channel.close(cause)
            }

            enum class Mode {
                /**
                 * Watches only the given file
                 */
                SingleFile,

                /**
                 * Watches changes in the given directory, changes in subdirectories will be
                 * ignored
                 */
                SingleDirectory,

                /**
                 * Watches changes in subdirectories
                 */
                Recursive
            }
        }

        var dirty = SimpleStringProperty()
        val watchChannel = this.asWatchChannel()

        fun save() {
            writeText(dirty.value)
        }

        fun populate() : Set<ProjectFile>? {
            val col : MutableSet<ProjectFile> = mutableSetOf()
            return if (isDirectory ) {
                walkTopDown().filter { file -> file != this && file.parentFile == this}.forEach {
                    col.add(ProjectFile(it.absolutePath))
                }
                col
            } else
                null
        }

        fun difference() : Boolean {
            return if (isFile)
                readText() != dirty.value
            else
                false
        }

        fun bind(other: SimpleStringProperty) {
            dirty.bindBidirectional(other)
        }
    }

    init {
        treeview.prefWidth = 300.0
        treeview.onUserSelect { code.updateText() }
        treeview.isEditable = true
        /*Timer().schedule( 0, 5) {
            Platform.runLater(Runnable {
                updateTreeView()
            })}*/
    }

    fun open() {
        val path = chooseDirectory("Select Working Directory")?.absolutePath
        path?.let {
            project = ProjectFile(it)
            updateTreeView()
        }
    }

    fun asWatchChannel(): ProjectFile.KWatchChannel {
        return project.watchChannel
    }

    fun updateTreeView() {

        treeview.refresh()
        treeview.root = TreeItem(project)
        treeview.minWidth = 300.0
        treeview.populate {
            it.value.populate()
            /*if (it.value.isDirectory )
                it.value.walkTopDown().filter { file -> file != it.value }.toHashSet()
            else
                null*/

        }

        /*treeview.cellFormat {
            println(it.name)
            val dirtyIndicator = if (it.difference()) "*" else ""
            text = it.name + dirtyIndicator
            textFill = if (it.difference()) {
                Color.RED
            } else {
                Color.BLACK
            }
        }*/

        treeview.setCellFactory {
            ProjectCell(this)
        }

        if (treeview.selectedValue == null && treeview.root.children.size > 0)
            treeview.selectionModel.select(treeview.root.children.first())
    }

    fun save() {
        if (code.codeArea.isFocused)
        {
            getActiveFile()?.let {
                if (it.isFile) {
                    it.writeText(getActiveCode())
                    alert.outputMessage("File ${it.absolutePath} saved.")
                    treeview.refresh()
                }
            }
        }
    }

    fun getActiveFile(): ProjectFile? {
        return treeview.selectedValue
    }

    fun getActiveCode(): String {
        return if (treeview.selectedValue != null) {
            if (treeview.selectedValue!!.isFile)
                treeview.selectedValue!!.dirty.value
            else
                ""
        }
        else
            ""
    }

    fun selectFile(file : File) {
        listTree.findLast { it.value == file }?.let {
            treeview.selectionModel.selectedItems.removeAll()
            treeview.selectionModel.select(treeview.getRow(it as TreeItem<ProjectFile>))
        }
        code.updateText()
    }

    private fun recursiveSearch(parentNode: TreeItem<ProjectFile>) : MutableList<TreeItem<ProjectFile>> {
        val files = mutableListOf<TreeItem<ProjectFile>>()
        for (node in parentNode.children) {
            if (node.value.isFile)
                files.add(node)
            else
                files.plus(recursiveSearch(node))
        }
        return files
    }

    fun getFiles(): List<ProjectFile> {
        if (treeview.root == null)
            return listOf()

        listTree.clear()
        listTree.addAll(0, recursiveSearch(treeview.root))
        val files = mutableListOf<ProjectFile>()
        listTree.forEach { files.add(it.value) }
        return files
    }

    fun getTreeItems(): List<TreeItem<ProjectFile>> {
        return listTree
    }

    fun generateWelcomeCode(): String {
        return  "class C0 is_a PN\n" +
                "\tobject\n" +
                "        trans t4\n" +
                "            precond p4((x, #fail))\n" +
                "            postcond p3(x)\n" +
                "        trans t2\n" +
                "            cond p2(o)\n" +
                "            precond p3(x)\n" +
                "            action {y := o waitFor: x}\n" +
                "            postcond p4((x, y))\n" +
                "        trans t1\n" +
                "            precond p1(#e)\n" +
                "            action {o := C1 new.}\n" +
                "            postcond p2(o)\n" +
                "        trans t3\n" +
                "            cond p2(o)\n" +
                "            guard {o state: x. x >= 3}\n" +
                "            action {o reset.}\n" +
                "        place p1(2‘#e)\n" +
                "        place p2()\n" +
                "        place p4()\n" +
                "        place p3(1, 2)\n" +
                "\n" +
                "class C1 is_a PN\n" +
                "    object\n" +
                "        place p(0)\n" +
                "        trans t\n" +
                "            precond p(x)\n" +
                "            action {y := x + 1.}\n" +
                "            postcond p(y)\n" +
                "    method waitFor: x\n" +
                "        place return()\n" +
                "        place x()\n" +
                "        trans t1\n" +
                "            cond p(y)\n" +
                "            precond x(x)\n" +
                "            guard {x < y}\n" +
                "            postcond return(#fail)\n" +
                "        trans t2\n" +
                "            precond x(x), p(x)\n" +
                "            postcond return(#success), p(0)\n" +
                "    method reset\n" +
                "        place return()\n" +
                "        trans t\n" +
                "            precond p(x)\n" +
                "            postcond return(#e), p(0)\n" +
                "    sync state: x\n" +
                "        cond p(x)\n"
    }

    fun generateNewClassCode(name : String): String {
        return  "class ${name} is_a PN\n" +
                "\tobject\n" +
                "\n"
    }

}