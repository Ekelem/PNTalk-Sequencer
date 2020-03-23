package edu.pntalk.sequencer.model

import javafx.application.Platform
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.shape.Circle
import javafx.scene.transform.Scale
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import tornadofx.*
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.io.File
import java.util.concurrent.TimeUnit


@Serializable
data class ArchiveMessage (val id: Int = 0, val message_name: String = "", val caller_instance: String = "", val caller_class: String = "",
                            val receiver_instance: String = "", val receiver_class: String = "", val respond_to: Int = 0)

@Serializable
data class ArchiveTransStart (val transition_name: String = "", val instance: String = "", val instance_class: String= "", val id: Int= 0)

@Serializable
data class ArchiveTransEnd (val id: Int= 0, val changelog: List<Map<String, List<ArchiveValue>>>)

@Serializable
data class ArchiveValue (val type: Int= 0, val value: String)

@Serializable
data class ArchiveStep (val messages: List<ArchiveMessage>, val  transition_starts: List<ArchiveTransStart>, val transition_ends: List<ArchiveTransEnd>)

@Serializable
data class ArchiveInitial (val instance: String = "main", val cls: String, val places: List<Map<String, List<ArchiveValue>>>)

@Serializable
data class Archive (val steps: List<ArchiveStep>, val initial: List<ArchiveInitial>)

class Place(name : String, type : Int, value : String) {
    val nameProperty = SimpleStringProperty(name)
    var name by nameProperty

    val typeProperty = SimpleIntegerProperty(type)
    var type by typeProperty

    val valueProperty = SimpleStringProperty(value)
    var value by valueProperty
}

class ZoomingPane : Pane() {
    private val zoomFactor: DoubleProperty = SimpleDoubleProperty(1.0)
    /*override fun layoutChildren() {
        val pos = Pos.TOP_LEFT
        val width = width
        val height = height
        val top = insets.top
        val right = insets.right
        val left = insets.left
        val bottom = insets.bottom
        val contentWidth = (width - left - right) / zoomFactor.get()
        val contentHeight = (height - top - bottom) / zoomFactor.get()
        layoutInArea(content, left, top,
                contentWidth, contentHeight, 0.0, null,
                pos.hpos,
                pos.vpos)
    }*/

    fun getZoomFactor(): Double {
        return zoomFactor.get()
    }

    fun setZoomFactor(zoomFactor: Double?) {
        this.zoomFactor.set(zoomFactor!!)
    }

    fun zoomFactorProperty(): DoubleProperty {
        return zoomFactor
    }

    init {
        //scene.setOnMouseClicked { print("hello") }
        //children.add(content)
        val scale = Scale(1.0, 1.0)
        //content.transforms.add(scale)
        zoomFactor.addListener(javafx.beans.value.ChangeListener { _: ObservableValue<out Number>, _: Number, newValue: Number ->
            scale.x = newValue.toDouble()
            scale.y = newValue.toDouble()
            requestLayout()
        })
        MouseWheelListener { event: MouseWheelEvent ->
            print("hello")
            setZoomFactor(getZoomFactor() + event.unitsToScroll)
        }

    }
}

class MainModel : ViewModel() {

    val file = File("code.txt")
    val output = File("output.btw")
    val sfile = File("scenario.yaml")
    val seqDiag : Pane = Pane()
    val input : ZoomingPane = ZoomingPane()
    val floatingY : Pane = Pane()
    val floatingX : Pane = Pane()
    val code = SimpleStringProperty("Code")
    val progressComment = SimpleStringProperty("Pipeline not running")
    val scenario = SimpleStringProperty("Example Scenario")
    val usingScenario = SimpleBooleanProperty()
    val completion = SimpleDoubleProperty()
    var preview : List<Place> = listOf(Place("t1", 0, "1, 2")) //mock

    var steps: Int = 5
    var validCode : Boolean = false

    val frontend : VisualModel = VisualModel(seqDiag, input, floatingX, floatingY)

    init {
        input.setPrefSize(500.0, 600.0)
        input.addChildIfPossible(seqDiag)
        input.addChildIfPossible(floatingX)
        input.addChildIfPossible(floatingY)
        parse() //Mock
        seqDiag.setMinSize(30000.0, 30000.0)
        seqDiag.style = "-fx-background-color: #D3D3D333,\n" +
                "linear-gradient(from 0.5px 0px to 10.5px 0px, repeat, LightGray 5%, transparent 5%),\n" +
                "linear-gradient(from 0px 0.5px to 0px 10.5px, repeat, LightGray 5%, transparent 5%);"

    }

    fun verify() : Boolean{
        file.writeText(code.value)
        val proc = ProcessBuilder("./Translator2", "-f","code.txt")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(30, TimeUnit.SECONDS)
        validCode = proc.exitValue() == 0
        if (validCode) {
            output.writeText(proc.inputStream.bufferedReader().readText())
            print("verified")
            print(output.readText())
            return true
        }
        else
            print(proc.inputStream.bufferedReader().readText())
            return false
    }

    @UnstableDefault
    fun simulate(steps : Int) {
        completion.set(0.1)
        Platform.runLater {
            progressComment.set("Verifying PNTalk code")
        }
        if (!verify()) {
            print("translator Error")
            return
        }
        completion.set(0.6)
        Platform.runLater {
            progressComment.set("Computing Simulation steps..")
        }
        sfile.writeText(scenario.get())

        val proc = startVirtualMachine()
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        val json = Json.nonstrict
        proc.waitFor(60, TimeUnit.SECONDS)

        completion.set(0.8)
        Platform.runLater {
            progressComment.set("Decoding Simulation..")
        }
        val jsonString = proc.inputStream.bufferedReader().readText()
        completion.set(0.9)
        Platform.runLater {
            progressComment.set("Generating Sequence Diagram..")
        }
        println(jsonString)
        val archive = json.parse(Archive.serializer(), jsonString)
        frontend.draw(archive)
        completion.set(1.0)
        Platform.runLater {
            progressComment.set("Done.")
        }
    }

    fun startVirtualMachine(): ProcessBuilder {
        return if (usingScenario.get()) {
            ProcessBuilder("./VM2", "-f","output.btw","-c","scenario.yaml","-s", steps.toString())
        }
        else {
            ProcessBuilder("./VM2", "-f","output.btw", "-s", steps.toString())
        }
    }



    fun parse() {
        // Mock
        code.value = "main C0\n" +
                "\n" +
                "class C0 is_a PN\n" +
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

        scenario.value = """main-2:
  class: C0
  places:
    t: 3"""
    }


}