package edu.pntalk.sequencer.model

import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.reactfx.Subscription
import org.reactfx.util.Try
import tornadofx.*
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern

class CodeController: Controller() {

    val project : ProjectController by inject()
    val diagram : DiagramController by inject()
    val codeArea = CodeArea()
    val scenarioArea = CodeArea()
    private val code = SimpleStringProperty()
    val executor = Executors.newSingleThreadExecutor()
    val codeLabels = CodeLabels(project.getFiles())

    init {
        importStylesheet("/syntax-highlight.css")
        codeArea.onMouseClicked = EventHandler<MouseEvent> {
            computeHighlighting(codeArea.text)?.let { applyHighlighting(it) }
        }


        code.bind(codeArea.textProperty())
        code.onChange {
            project.getActiveFile()?.let { file ->
                file.dirty.value = it
            }
        }
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        codeArea.setPrefSize(500.0, 600.0)
        codeArea.replaceText(0, 0, project.getActiveCode())

        scenarioArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        scenarioArea.setPrefSize(500.0, 600.0)
        //subscription()
    }


    class CodeLabels(val fileList: List<ProjectController.ProjectFile>) {
        var main : String = "main"
        val files = mutableMapOf<String, FileLabelData>()

        init {
            generateLabels()
        }

        fun getLine(file : File, characterPosition : Int) : Int {
            var charCount = characterPosition
            for ((lineCount, line) in file.readLines().withIndex())
            {
                if (charCount <= 0)
                    return lineCount

                charCount-=line.length + 1
            }
            return 0
        }

        fun updateLabels(file : ProjectController.ProjectFile) {
            val text = file.dirty.value
            val classNames = "class (.*) is_a PN".toRegex()
            val classMap = mutableMapOf<String, ClassLabelData>()
            val matches = classNames.findAll(text)
            val borders = mutableListOf<Pair<String, Int>>()
            for (match in matches) {    // class definitions
                borders.add(Pair(match.groupValues[1], match.range.first))
            }

            for (match in matches) {
                var borderNext : Int? = null
                var next = false
                for (border in borders) {
                    if (next)
                        borderNext = border.second
                    if (border.first == match.groupValues[1])
                        next = true
                }

                if (borderNext == null)
                    borderNext = (file.dirty.value.length - 1).toInt()

                val range = IntRange(match.range.first, borderNext)
                classMap[match.groupValues[1]] = ClassLabelData(range, getLine(file, match.range.first))
            }
            files[file.name] = FileLabelData(file, classMap)

            for (cls in files[file.name]!!.classes.keys) {
                val transNames = "trans (\\w+)".toRegex()
                var matches = transNames.findAll(text.subSequence(files[file.name]!!.classes[cls]!!.range))
                for (match in matches) {    // transition definitions
                    files[file.name]!!.classes[cls]!!.addTransition(Pair(match.groupValues[1], TransLabelData(match.range)))
                }

                val placeNames = "place (\\w+)\\(".toRegex()
                matches = placeNames.findAll(text.subSequence(files[file.name]!!.classes[cls]!!.range))
                for (match in matches) {    // places definitions
                    files[file.name]!!.classes[cls]!!.addPlace(Pair(match.groupValues[1], PlaceLabelData(match.range)))
                }

                val syncsNames = "sync (\\w+):".toRegex()
                matches = syncsNames.findAll(text.subSequence(files[file.name]!!.classes[cls]!!.range))
                for (match in matches) {    // places definitions
                    files[file.name]!!.classes[cls]!!.addSync(Pair(match.groupValues[1], SyncLabelData(match.range, getLine(file, match.range.first))))
                }

                val methodNames = "method (\\w+)".toRegex()
                matches = methodNames.findAll(text.subSequence(files[file.name]!!.classes[cls]!!.range))
                for (match in matches) {    // places definitions
                    files[file.name]!!.classes[cls]!!.addMethod(Pair(match.groupValues[1], MethodLabelData(match.range, getLine(file, match.range.first))))
                }
            }
        }

        /**
         * Generate struct consisting of key pair values,
         * where keys are names of files, classes, transitions, places and methods
         * and keys are range where can be definition found in code.
         */
        fun generateLabels() {
            for (file in fileList) {    // Parse every file
                updateLabels(file)
            }
        }

        fun getClassCodeRange(name : String) : LineCodeData? {
            for (file in files) {
                if (file.value.classes[name] != null) {
                    return LineCodeData(
                            file.value.file,
                            file.value.classes[name]!!.range,
                            file.value.classes[name]!!.line)
                }
            }
            return null // Class not found
        }

        fun getCreateCodeRange(parent : String, child : String) : LineCodeData? {
            val from = getClassCodeRange(parent)
            from?.let {from ->
                val result = "$child new.".toRegex().find(from.file.dirty.value.subSequence(from.range))
                result.let {
                    return LineCodeData(from.file,
                            IntRange(from.range.first + it!!.range.first,
                                from.range.first + it!!.range.last),
                            getLine(from.file, from.range.first + it!!.range.first)
                    )
                }
            }
            return null
        }

        fun getMessageCodeRange(name : String, receiver : String) : LineCodeData? {
            for (file in files) {
                file.value.classes[receiver]?.let { cls ->
                    for (method in cls.methods.keys) {
                        if (method == name)
                            return LineCodeData(
                                    file.value.file,
                                    IntRange(cls.methods[method]!!.range.first + cls.range.first,
                                            cls.methods[method]!!.range.last + cls.range.first),
                                    cls.methods[method]!!.line
                            )
                    }

                    for (sync in cls.syncs.keys) {
                        if (sync == name)
                            return LineCodeData(
                                    file.value.file,
                                    IntRange(cls.syncs[sync]!!.range.first + cls.range.first,
                                            cls.syncs[sync]!!.range.last + cls.range.first),
                                    cls.syncs[sync]!!.line
                            )
                    }
                }
            }
            return null // Message not found
        }

        class FileLabelData(val file : ProjectController.ProjectFile, classMap: Map<String, ClassLabelData>) {
            val classes = classMap
        }

        class LineCodeData(val file : ProjectController.ProjectFile, val range: IntRange, val line : Int) {

        }

        class ClassLabelData(val range: IntRange, val line : Int) {
            val transitions = mutableMapOf<String, TransLabelData>()
            val places = mutableMapOf<String, PlaceLabelData>()
            val syncs = mutableMapOf<String, SyncLabelData>()
            val methods = mutableMapOf<String, MethodLabelData>()

            fun addTransition(trans : Pair<String, TransLabelData>) {
                transitions[trans.first] = trans.second
            }

            fun addPlace(place : Pair<String, PlaceLabelData>) {
                places[place.first] = place.second
            }

            fun addSync(sync : Pair<String, SyncLabelData>) {
                syncs[sync.first] = sync.second
            }

            fun addMethod(method : Pair<String, MethodLabelData>) {
                methods[method.first] = method.second
            }
        }

        class TransLabelData(val range : IntRange)

        class PlaceLabelData(val range: IntRange)

        class SyncLabelData(val range: IntRange, val line : Int)

        class MethodLabelData(val range: IntRange, val line: Int)
    }

    fun setText(text: String) {
        codeArea.replaceText(text)
        computeHighlighting(codeArea.text)?.let { applyHighlighting(it) }
    }

    fun updateText() {
        codeArea.replaceText(project.getActiveCode())
        computeHighlighting(codeArea.text)?.let { applyHighlighting(it) }
    }

    fun highlightSpan(step : Int, instance: String, cls: String) {
        println(step)
    }

    fun highlightConnection(message : String, receiver: String, caller: String) {
        if (message == "<<create>>") {
            try {
                val result = codeLabels.getCreateCodeRange(caller, receiver)
                if (result != null) {
                    project.selectFile(result.file)
                    var range : IntRange = result.range
                    var line : Int = result.line

                    val pattern = computeClassPattern(receiver)
                    val matcher = pattern.matcher(codeArea.text.subSequence(range.first, range.last + 1))
                    val spansBuilder = StyleSpansBuilder<Collection<String>>()
                    var lastKwEnd = 0
                    while (matcher.find()) {
                        val style = PNConfiguration.highlightClass.findLast { matcher.group(it) != null }?.toLowerCase()?.plus("-highlight").orEmpty()
                        spansBuilder.add(Collections.singleton("highlight"), matcher.start() - lastKwEnd)
                        spansBuilder.add(Collections.singleton(style), matcher.end() - matcher.start())
                        lastKwEnd = matcher.end()
                    }

                    codeArea.setStyleSpans(range.first, spansBuilder.create())
                    codeArea.showParagraphAtTop(line)
                }
            }
            catch (e:StringIndexOutOfBoundsException) {
                // not exists anymore, do nothing
            }
        }
        else {
            try {
                var methodName = message
                var clsName = receiver
                val responseRegex = "Response to (\\w+)".toRegex()
                if (responseRegex.containsMatchIn(message)) {
                    println(responseRegex.find(message)!!.groups[1]!!.value)
                    methodName = responseRegex.find(message)!!.groups[1]!!.value
                    clsName = caller
                }
                val result = codeLabels.getMessageCodeRange(methodName, clsName)
                if (result != null) {
                    project.selectFile(result.file)
                    var range : IntRange = result.range
                    var line : Int = result.line

                    val pattern = computeClassPattern(receiver)
                    val matcher = pattern.matcher(codeArea.text.subSequence(range.first, range.last + 1))
                    val spansBuilder = StyleSpansBuilder<Collection<String>>()
                    var lastKwEnd = 0
                    while (matcher.find()) {
                        val style = PNConfiguration.highlightClass.findLast { matcher.group(it) != null }?.toLowerCase()?.plus("-highlight").orEmpty()
                        spansBuilder.add(Collections.singleton("highlight"), matcher.start() - lastKwEnd)
                        spansBuilder.add(Collections.singleton(style), matcher.end() - matcher.start())
                        lastKwEnd = matcher.end()
                    }

                    codeArea.setStyleSpans(range.first, spansBuilder.create())
                    codeArea.showParagraphAtTop(line)
                }
            }
            catch (e:StringIndexOutOfBoundsException) {
                // not exists anymore, do nothing
            }
        }
    }

    fun highlightClass(name : String) {
        codeLabels.generateLabels()
        val result = codeLabels.getClassCodeRange(name)
        if (result != null)
        {
            try {
                project.selectFile(result.file)
                val pattern = computeClassPattern(name)
                val matcher = pattern.matcher(codeArea.text.subSequence(result.range.first, result.range.last))
                val spansBuilder = StyleSpansBuilder<Collection<String>>()
                var lastKwEnd = 0
                while (matcher.find()) {
                    val style = PNConfiguration.highlightClass.findLast { matcher.group(it) != null }?.toLowerCase()?.plus("-highlight").orEmpty()
                    spansBuilder.add(Collections.singleton("highlight"), matcher.start() - lastKwEnd)
                    spansBuilder.add(Collections.singleton(style), matcher.end() - matcher.start())
                    lastKwEnd = matcher.end()
                }

                codeArea.setStyleSpans(result.range.first, spansBuilder.create())
                codeArea.showParagraphAtTop(result.line)
            }
            catch (e:StringIndexOutOfBoundsException) {
                // not exists, do nothing
            }
        }
    }

    fun getCode():String {
        val stringBuilder = StringBuilder()
        for (file in project.getFiles()) {
            stringBuilder.append(file.dirty.value)
        }
        return stringBuilder.toString()
    }


    private fun applyHighlighting(highlighting: StyleSpans<Collection<String>>) {
        codeArea.setStyleSpans(0, highlighting)
    }

    private fun computeHighlightingAsync(): Task<StyleSpans<Collection<String?>?>?>? {
        val text = codeArea.text
        val task: Task<StyleSpans<Collection<String?>?>?> = object : Task<StyleSpans<Collection<String?>?>?>() {
            @Throws(Exception::class)
            override fun call(): StyleSpans<Collection<String?>?>? {
                return computeHighlighting(text) as StyleSpans<Collection<String?>?>?
            }
        }
        executor.execute(task)
        return task
    }

    private val KEYWORDS = arrayOf(
            "main", "class", "is_a PN","object", "precond", "postcond",
            "trans", "place", "guard", "cond", "#fail",
            "#success", "default", "new", "sync", "method",
            "action", "#e"
    )

    private val KEYWORD_PATTERN = "\\b(" + java.lang.String.join("|", *KEYWORDS) + ")\\b"
    private val PAREN_PATTERN = "\\(|\\)"
    private val BRACE_PATTERN = "\\{|\\}"
    private val BRACKET_PATTERN = "\\[|\\]"
    private val SEMICOLON_PATTERN = "\\;"
    private val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
    private val COMMENT_PATTERN = """
        //[^
        ]*|/\*(.|\R)*?\*/
        """.trimIndent()

    private val PATTERN = Pattern.compile(
            "(?<KEYWORD>$KEYWORD_PATTERN)|(?<PAREN>$PAREN_PATTERN)|(?<BRACE>$BRACE_PATTERN)|(?<BRACKET>$BRACKET_PATTERN)|(?<SEMICOLON>$SEMICOLON_PATTERN)|(?<STRING>$STRING_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)"
    )

    private fun getClassRange(text: String): Map<String, IntRange> {
        val table = mutableMapOf<String, IntRange>()
        val classNames = "class (.*) is_a PN".toRegex()
        val matches = classNames.findAll(text)
        for (match in matches) {
            codeLabels.files[""]
            table[match.groups[1]!!.value] = match.groups[0]!!.range
        }
        return table
    }

    private fun getTransRange(text: String): Map<String, IntRange> {
        val table = mutableMapOf<String, IntRange>()
        val transNames = "trans (.*)".toRegex()
        val matches = transNames.findAll(text)
        for (match in matches) {
            table[match.groups[1]!!.value] = match.groups[0]!!.range
        }
        return table
    }

    fun getClassNames() : Set<String> {
        project.getActiveFile()?.let {
            return codeLabels.files[it.name]!!.classes.keys
        }
        return setOf()
    }

    fun getTransNames(cls : String) : Set<String> {
        codeLabels.files[project.getActiveFile()?.name]?.classes?.get(cls)?.let {
            return it.transitions.keys
        }
        return setOf()
    }

    fun getPlaceNames(cls : String) : Set<String> {
        codeLabels.files[project.getActiveFile()?.name]?.classes?.get(cls)?.let {
            return it.places.keys
        }
        return setOf()
    }

    fun getSyncNames(cls : String) : Set<String> {
        codeLabels.files[project.getActiveFile()?.name]?.classes?.get(cls)?.let {
            return it.syncs.keys
        }
        return setOf()
    }

    fun getMethodNames(cls : String) : Set<String> {
        codeLabels.files[project.getActiveFile()?.name]?.classes?.get(cls)?.let {
            return it.methods.keys
        }
        return setOf()
    }

    fun getSyncNames() : Set<String> {
        val syncs = mutableSetOf<String>()
        project.getFiles().forEach { file ->
            codeLabels.files[file.name]?.classes?.forEach { cls ->
                syncs.addAll(cls.value.syncs.keys)
            }
        }
        return syncs
    }

    fun getMethodNames() : Set<String> {
        val methods = mutableSetOf<String>()
        project.getFiles().forEach { file ->
            codeLabels.files[file.name]?.classes?.forEach { cls ->
                methods.addAll(cls.value.methods.keys)
            }
        }
        return methods
    }


    fun subscription(): Subscription? {
        return codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(500))
                .supplyTask<Any>(Supplier (this::computeHighlightingAsync) as Supplier<Task<Any>>)
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap { t: Try<Any> ->
                    if (t.isSuccess) {
                        return@filterMap Optional.of(t.get())
                    } else {
                        t.failure.printStackTrace()
                        return@filterMap Optional.empty<Any>()
                    }
                }
                .subscribe(Consumer(this::applyHighlighting) as Consumer<in Any>)
    }

    private fun matchCodeArea(matcher : Matcher, border: Int, groups: List<String>, spansBuilder: StyleSpansBuilder<Collection<String>>) : Int{
        var lastKwEnd = border
        while (matcher.find()) {
            val style = groups.findLast { matcher.group(it) != null }?.toLowerCase().orEmpty()
            //val styleClass = if (matcher.group("KEYWORD") != null) "keyword" else if (matcher.group("PAREN") != null) "paren" else if (matcher.group("CLS") != null) "cls" else if (matcher.group("BRACE") != null) "brace" else if (matcher.group("BRACKET") != null) "bracket" else if (matcher.group("SEMICOLON") != null) "semicolon" else if (matcher.group("STRING") != null) "string" else if (matcher.group("COMMENT") != null) "comment" else null /* never happens */
            spansBuilder.add(Collections.emptyList(), matcher.start() - (lastKwEnd - border))
            spansBuilder.add(Collections.singleton(style), matcher.end() - matcher.start())
            lastKwEnd = matcher.end() + border
        }
        return lastKwEnd
    }

    fun computeGlobalPattern() : Pattern {
        val CLASS_PATTERN = "\\b(${getClassNames().joinToString("|")})\\b"

        return Pattern.compile(
                "(?<KEYWORD>$KEYWORD_PATTERN)|" +
                        "(?<CLS>$CLASS_PATTERN)|" +
                        "(?<BRACE>$BRACE_PATTERN)|" +
                        "(?<PAREN>$PAREN_PATTERN)|" +
                        "(?<BRACKET>$BRACKET_PATTERN)|" +
                        "(?<SEMICOLON>$SEMICOLON_PATTERN)|" +
                        "(?<COMMENT>$COMMENT_PATTERN)")
    }

    fun computeClassPattern( cls: String ) : Pattern {
        val CLASS_PATTERN = "\\b(${getClassNames().joinToString("|")})\\b"
        val TRANS_PATTERN = "\\b(${getTransNames(cls).joinToString("|")})\\b"
        val PLACE_PATTERN = "\\b(${getPlaceNames(cls).joinToString("|")})\\b"
        val SYNC_PATTERN = "\\b(${getSyncNames().joinToString("|")})\\b"
        val METHOD_PATTERN = "\\b(${getMethodNames().joinToString("|")})\\b"
        return Pattern.compile(
                "(?<KEYWORD>$KEYWORD_PATTERN)|" +
                        "(?<CLS>$CLASS_PATTERN)|" +
                        "(?<TRANS>$TRANS_PATTERN)|" +
                        "(?<PLACE>$PLACE_PATTERN)|" +
                        "(?<SYNC>$SYNC_PATTERN)|" +
                        "(?<METHOD>$METHOD_PATTERN)|" +
                        "(?<PAREN>$PAREN_PATTERN)|" +
                        "(?<BRACE>$BRACE_PATTERN)|" +
                        "(?<BRACKET>$BRACKET_PATTERN)|" +
                        "(?<SEMICOLON>$SEMICOLON_PATTERN)|" +
                        "(?<STRING>$STRING_PATTERN)|" +
                        "(?<COMMENT>$COMMENT_PATTERN)")
    }

    private fun computeHighlighting(text: String): StyleSpans<Collection<String>>? {
        project.getActiveFile()!!.dirty.value = codeArea.text
        project.treeview.refresh()
        //codeArea.replaceText(project.getActiveCode())
        codeLabels.updateLabels(project.getActiveFile()!!)
        codeLabels.files[project.getActiveFile()?.name]?.let {
            diagram.clearHighlightedNode()
            val globalPattern = computeGlobalPattern()
            var border = 0
            val spansBuilder = StyleSpansBuilder<Collection<String>>()
            for (cls in it.classes) {
                try {
                    var matcher = globalPattern.matcher(text.subSequence(border, cls.value.range.first))
                    border = matchCodeArea(matcher, border, PNConfiguration.highlightGlobal, spansBuilder)
                    val localPattern = computeClassPattern(cls.key)
                    matcher = localPattern.matcher(text.subSequence(border, cls.value.range.last - 1))
                    border = matchCodeArea(matcher, border, PNConfiguration.highlightClass, spansBuilder)
                }
                catch (e: StringIndexOutOfBoundsException) {
                    // not exists anymore, Do nothing
                }
            }
            spansBuilder.add(Collections.emptyList(), text.length - border)
            return spansBuilder.create()
        }

        return null
    }
}