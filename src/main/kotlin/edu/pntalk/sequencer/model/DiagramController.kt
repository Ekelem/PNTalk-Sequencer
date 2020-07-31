/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableValue
import javafx.geometry.Point2D
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import tornadofx.Controller
import tornadofx.addChildIfPossible
import tornadofx.clear
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.layout.StackPane
import javafx.scene.transform.Scale
import tornadofx.add
import java.awt.event.MouseWheelEvent

class DiagramController: Controller() {
    val code: CodeController by inject()
    val alert: AlertController by inject()
    val changelog: ChangelogController by inject()
    private val instanceMap = HashMap<String, PNObject>()
    private val messageMap = HashMap<Int, PNConnection>()
    private val responses = ArrayList<PNConnection>()
    private val spacers = ArrayList<PNStepSpacer>()
    var highlightedObject : Node? = null

    val diagramPane = Pane()
    val input = Pane()
    val floatingYPane : Pane = Pane()
    val floatingXPane : Pane = Pane()

    var dragStart: Point2D = Point2D(0.0, 0.0)
    var dragRoot: Point2D = Point2D(0.0, 0.0)
    var archive: Archive? = null

    init {
        input.setPrefSize(500.0, 600.0)
        input.add(floatingYPane)
        input.add(diagramPane)
        //input.add(floatingYPane)
        //input.add(floatingXPane)ti
        /*input.addChildIfPossible(diagramPane)
        input.addChildIfPossible(floatingXPane)
        input.addChildIfPossible(floatingYPane)*/
        input.onMouseClicked = EventHandler {
            println("Hell")
            //diagramPane.fireEvent()
        }

        diagramPane.onMouseClicked = EventHandler<MouseEvent> { print("Hello")}
        diagramPane.setMinSize(30000.0, 30000.0)
        diagramPane.style = "-fx-background-color: #D3D3D333,\n" +
                "linear-gradient(from 0.5px 0px to 10.5px 0px, repeat, LightGray 5%, transparent 5%),\n" +
                "linear-gradient(from 0px 0.5px to 0px 10.5px, repeat, LightGray 5%, transparent 5%);"

    }

    fun getStep(time: Double) : Int {
        var step = 0
        for (spacer in spacers) {
            if (spacer.time < time)
                step = spacer.id + 1
            else
                break
        }
        return step
    }

    fun printSpacers() {
        for (spacer in spacers) {
            alert.outputMessage("from 0.0")
            alert.outputMessage("${spacer.id}: to ${spacer.time}")
        }
    }

    fun setHighlightedNode(node: Node) {
        if (highlightedObject != null) {
            highlightedObject!!.effect = null
        }
        node.effect = PNConfiguration.highlightShadow
        highlightedObject = node
    }

    fun clearHighlightedNode() {
        if (highlightedObject != null) {
            highlightedObject!!.effect = null
        }
        highlightedObject = null
    }

    fun isValid(): Boolean {
        return archive != null
    }

    fun getObject(name : String) : PNObject? {
        return instanceMap[name]
    }

    fun getObjects() : List<PNObject> {
        return instanceMap.values.toList()
    }

    fun getMessages() : List<PNConnection> {
        return messageMap.values.toList() + responses.toList()
    }

    fun createObject(name: String, classname: String, creation : Double) {
        val visual = PNObject(name, classname,instanceMap.count()*(PNConfiguration.instanceSpace + PNConfiguration.instanceSize.x) + PNConfiguration.offset.x,creation, this)
        instanceMap[name] = visual
    }

    fun createMessage(id: Int, messageName: String, caller: String, receiver: String, time: Double) {
        instanceMap[caller]?.let { from -> instanceMap[receiver]?.let { to ->
            messageMap[id] = (PNConnection(messageName, from,to, time, this)) } }
    }

    fun createMessage(respondTo: Int, time: Double) {
        messageMap[respondTo]?.let { original ->
            responses.add(PNConnection("Response to ${original.messageName.text}", original.receiver, original.caller, time, this,true))
        }
    }

    private fun createMain(initial: List<ArchiveInitial>) {
        for (starter in initial) {
            if (starter.creation == 0)
                createObject(starter.instance, starter.cls, 0.0)
        }
    }

    private fun registerBgMovement() {
        input.onMousePressed = EventHandler { event: MouseEvent ->
            dragStart = Point2D(event.screenX, event.screenY)
        }
        input.onMouseDragged = EventHandler { event: MouseEvent ->
            val offset = Point2D(event.screenX, event.screenY).subtract(dragStart)
            val newPosition = Point2D(offset.x + dragRoot.x, offset.y + dragRoot.y)
            if (diagramPane.layoutBounds.contains(Point2D(-newPosition.x, -newPosition.y))) {
                diagramPane.relocate( newPosition.x, newPosition.y)
                floatingXPane.translateX = newPosition.x
                floatingYPane.translateY = newPosition.y
            }
        }
        input.onMouseReleased = EventHandler { event: MouseEvent ->
            dragRoot = Point2D(diagramPane.layoutX, diagramPane.layoutY)
        }
    }

    fun draw(backend: Archive) {
        instanceMap.clear()
        messageMap.clear()
        responses.clear()
        spacers.clear()
        Platform.runLater {
            floatingYPane.clear()
            diagramPane.clear()
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
                    messageMap[message.respond_to]?.confirmReply()
                    messageMap[message.respond_to]?.receiver?.stopSpan(simulationTime)
                }
            }
            spacers.add(PNStepSpacer(stepCount, simulationTime))
        }

        for (init in backend.initial) {
            instanceMap[init.instance]?.initPlaces(init.places)
            if (init.creation == 0) {
                instanceMap[init.instance]?.makeMain()
                instanceMap[init.instance]?.killSpan(simulationTime)
            }
        }

        Platform.runLater {
            for (instance in instanceMap) {
                instance.value.extendDuration(simulationTime)
                diagramPane.addChildIfPossible(instance.value.root)
            }
            for (message in messageMap) {
                diagramPane.addChildIfPossible(message.value.root)
            }
            for (response in responses) {
                diagramPane.addChildIfPossible(response.root)
            }
            for (spacer in spacers) {
                floatingYPane.addChildIfPossible(spacer.root)
            }
        }
        archive = backend
    }


}