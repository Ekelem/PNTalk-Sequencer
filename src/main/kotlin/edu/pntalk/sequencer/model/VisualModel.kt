package edu.pntalk.sequencer.model

import edu.pntalk.sequencer.view.ErrorFragment
import edu.pntalk.sequencer.view.MainView
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import org.fxmisc.richtext.CodeArea
import tornadofx.addChildIfPossible
import tornadofx.clear
import java.io.OutputStreamWriter
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.count
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.collections.withIndex

import org.w3c.dom.Document;
import java.awt.BasicStroke
import java.awt.Shape
import java.awt.Stroke


class VisualModel (main: Pane, input: Pane, floatingX: Pane, floatingY: Pane) {
    val instances = HashMap<String, PNObject>()
    val messages = HashMap<Int, PNConnection>()
    val responses = ArrayList<PNConnection>()
    val spacers = ArrayList<PNStepSpacer>()
    val inputPane = input
    val mainPane = main
    val floatingXPane = floatingX
    val floatingYPane = floatingY
    lateinit var supervisor : MainView

    var dragStart: Point2D = Point2D(0.0, 0.0)
    var dragRoot: Point2D = Point2D(0.0, 0.0)

    private fun registerBgMovement() {
        inputPane.onMousePressed = EventHandler { event: MouseEvent ->
            dragStart = Point2D(event.screenX, event.screenY)
        }
        inputPane.onMouseDragged = EventHandler { event: MouseEvent ->
            val offset = Point2D(event.screenX, event.screenY).subtract(dragStart)
            val newPosition = Point2D(offset.x + dragRoot.x, offset.y + dragRoot.y)
            if (mainPane.layoutBounds.contains(Point2D(-newPosition.x, -newPosition.y))) {
                mainPane.relocate( newPosition.x, newPosition.y)
                floatingXPane.translateX = newPosition.x
                floatingYPane.translateY = newPosition.y
            }
        }
        inputPane.onMouseReleased = EventHandler { event: MouseEvent ->
            dragRoot = Point2D(mainPane.layoutX, mainPane.layoutY)
        }
    }

    fun errorMessage(errorCode: Int, errorMessage: String) {
        ErrorFragment(errorCode, errorMessage).openWindow()
    }


    /*fun draw(backend: Archive) {
        instances.clear()
        messages.clear()
        responses.clear()
        spacers.clear()
        Platform.runLater {
            floatingYPane.clear()
            mainPane.clear()
        }

        registerBgMovement()
        createMain(backend.initial)
        var simulationTime = 0.0

        for ((stepCount, step) in backend.steps.withIndex()) {
            for (message in step.messages) {
                simulationTime += 40.0
                if (message.respond_to == 0) {
                    if (message.message_name == "create instance") {
                        createObject(message.receiver_instance, message.receiver_class, simulationTime)
                        createMessage(message.id, "<<create>>", message.caller_instance, message.receiver_instance, simulationTime)
                    }
                    else {
                        createMessage(message.id, message.message_name, message.caller_instance, message.receiver_instance, simulationTime)
                    }
                }
                else {
                    createMessage(message.respond_to, simulationTime)
                    messages[message.respond_to]?.confirmReply()
                    messages[message.respond_to]?.receiver?.stopSpan(simulationTime)
                }
            }
            spacers.add(PNStepSpacer(stepCount, simulationTime))
        }

        for (init in backend.initial) {
            instances[init.instance]?.initPlaces(init.places)
            instances[init.instance]?.makeMain()
            instances[init.instance]?.killSpan(simulationTime)
        }

        Platform.runLater {
            for (instance in instances) {
                instance.value.extendDuration(simulationTime)
                mainPane.addChildIfPossible(instance.value.root)
            }
            for (message in messages) {
                mainPane.addChildIfPossible(message.value.root)
            }
            for (response in responses) {
                mainPane.addChildIfPossible(response.root)
            }
            for (spacer in spacers) {
                floatingYPane.addChildIfPossible(spacer.root)
            }
        }

    }*/

    fun exportSVG() {
        val domImpl = GenericDOMImplementation.getDOMImplementation()
        // Create an instance of org.w3c.dom.Document.
        val svgNS = "http://www.w3.org/2000/svg"
        val document: Document = domImpl.createDocument(svgNS, "svg", null)

        val svgGenerator = SVGGraphics2D(document)
        svgGenerator.stroke = BasicStroke(0.3F)
        svgGenerator.drawLine(0, 1, 8, 1)

        val useCSS = true // we want to use CSS style attributes
        svgGenerator.stream("export.svg", useCSS)
    }

    /*fun createObject(name: String, classname: String, creation : Double) {
        val visual = PNObject(name, classname,instances.count()*(PNConfiguration.instanceSpace + PNConfiguration.instanceSize.x) + PNConfiguration.offset.x,creation, )
        instances[name] = visual
    }*/

    /*fun createMessage(id: Int, messageName: String, caller: String, receiver: String, time: Double) {
        instances[caller]?.let { from -> instances[receiver]?.let { to ->
            messages[id] = (PNConnection(messageName, from,to, time, this)) } }
    }

    fun createMessage(respondTo: Int, time: Double) {
        messages[respondTo]?.let { original ->
            responses.add(PNConnection("Response to ${original.messageName.text}", original.receiver, original.caller, time, true))
        }
    }*/

    /*private fun createMain(initial: List<ArchiveInitial>) {
        for (starter in initial) {
            createObject(starter.instance, starter.cls, 0.0)
            //TODO: places
        }
    }*/
}
