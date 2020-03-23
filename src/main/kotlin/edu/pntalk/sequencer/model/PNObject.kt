package edu.pntalk.sequencer.model

import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import tornadofx.*
import java.util.*

class PNObject(var name : String, val classname : String, val x : Double, val creation : Double) {
    val root : Group = Group()
    val header : StackPane = StackPane()
    val rect : Rectangle = Rectangle(PNConfiguration.instanceSize.x, PNConfiguration.instanceSize.y)
    var lifeline : Line = Line(PNConfiguration.instanceSize.x/2,
            PNConfiguration.instanceSize.y,
            PNConfiguration.instanceSize.x/2,
            PNConfiguration.instanceSize.y)
    val spans : LinkedList<PNSpan> = LinkedList()
    var spansCount : Int = 0
    var spansStart : Double = 0.0
    val instanceName : Text = Text(rect.width/2, rect.height/2, "$name:$classname")
    val initial = LinkedList<Place>()
    init {
        root.translateX = x
        root.translateY = PNConfiguration.offset.y + creation + rect.height/2
        rect.fill = Color.WHITE
        rect.stroke = Color.BLACK
        rect.strokeWidth = 2.0
        lifeline.strokeDashArray.addAll(5.0, 4.0)
        lifeline.strokeWidth = 1.0
        instanceName.setUnderline(true)
        instanceName.x = (instanceName.x - instanceName.layoutBounds.width/2)
        header.addChildIfPossible(rect)
        header.addChildIfPossible(instanceName)
        root.addChildIfPossible(header)
        root.addChildIfPossible(lifeline)
    }

    fun extendDuration(duration: Double) {
        lifeline.endY = rect.height+duration-creation-rect.height/2
    }

    fun makeMain() {
        spansCount = 666
        spansStart = PNConfiguration.instanceSize.y/2
        initial.clear()
    }

    fun startSpan(time : Double) {
        if (spansCount == 0)
            spansStart = time

        spansCount++
    }

    fun stopSpan(time : Double) {

        if (spansCount == 1) {
            val span = PNSpan(spansStart - creation, time - creation)
            root.addChildIfPossible(span.rect)
            spans.add(span)
        }

        spansCount--
    }

    fun killSpan(time : Double) {
        if (spansCount > 0) {
            val span = PNSpan(spansStart - creation, time - creation)
            root.addChildIfPossible(span.rect)
            spans.add(span)
        }

        spansCount = 0
    }

    fun initPlaces(places: List<Map<String, List<ArchiveValue>>>) {
        for (place in places) {
            for (init in place) {
                var str = ""
                for (value in init.value)
                    str += value.value

                initial.add(Place(init.key, 0, str))
            }
        }
    }
}