package edu.pntalk.sequencer.model

import javafx.event.EventHandler
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import org.apache.batik.svggen.SVGGraphics2D
import tornadofx.*
import java.awt.BasicStroke
import java.awt.Graphics
import java.util.*

class PNObject(var name : String, val classname : String, val x : Double, val creation : Double, val diagram : DiagramController) {
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
    //val initial = LinkedList<Place>()
    val startPlaces = mutableListOf<SeparatePlace>()
    init {
        root.translateX = x
        root.translateY = PNConfiguration.offset.y + creation + rect.height/2
        rect.fill = Color.WHITE
        rect.stroke = Color.BLACK
        rect.strokeWidth = 2.0
        lifeline.strokeDashArray.addAll(5.0, 4.0)
        lifeline.strokeWidth = 1.0
        instanceName.isUnderline = true
        instanceName.x = (instanceName.x - instanceName.layoutBounds.width/2)
        header.addChildIfPossible(rect)
        header.addChildIfPossible(instanceName)
        header.onMouseClicked = EventHandler<MouseEvent> {
            diagram.code.highlightClass(classname)
            diagram.changelog.getState(0, name, classname)
            diagram.setHighlightedNode(header)
        }
        root.addChildIfPossible(header)
        root.addChildIfPossible(lifeline)
    }

    fun extendDuration(duration: Double) {
        lifeline.endY = rect.height+duration-creation-rect.height/2
    }

    fun makeMain() {
        spansCount = 666
        spansStart = PNConfiguration.instanceSize.y/2
    }

    fun startSpan(time : Double) {
        if (spansCount == 0)
            spansStart = time

        spansCount++
    }

    fun stopSpan(time : Double) {

        if (spansCount == 1) {
            val span = PNSpan(spansStart - creation, time - creation, name, classname, diagram)
            root.addChildIfPossible(span.rect)
            spans.add(span)
        }

        spansCount--
    }

    fun killSpan(time : Double) {
        if (spansCount > 0) {
            val span = PNSpan(spansStart - creation, time - creation, name, classname, diagram)
            root.addChildIfPossible(span.rect)
            spans.add(span)
        }

        spansCount = 0
    }

    fun initPlaces(places: List<Map<String, List<ArchiveValue>>>) {
        startPlaces.clear()
        for (place in places) {
            for (init in place) {
                val list = mutableListOf<SeparateValue>()
                for (term in init.value)
                    list.add(SeparateValue(term.value, term.type))

                startPlaces.add(SeparatePlace(init.key, list))
            }
        }
    }

    fun exportSVG(svg: SVGGraphics2D) {
        // Convert Javafx into SVG representation
        val rectCoordinates = Point2D(rect.x + root.translateX + header.translateX, rect.y + root.translateY + header.translateY)
        val labelCoordinates = Point2D(instanceName.x + root.translateX + header.translateX, instanceName.y + root.translateY + header.translateY)

        svg.color = java.awt.Color.WHITE
        svg.fillRect(rectCoordinates.x.toInt(), rectCoordinates.y.toInt(), rect.width.toInt(), rect.height.toInt())
        svg.color = java.awt.Color.BLACK
        svg.drawRect(rectCoordinates.x.toInt(), rectCoordinates.y.toInt(), rect.width.toInt(), rect.height.toInt())
        svg.stroke = BasicStroke(PNConfiguration.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0F, floatArrayOf(9f), 0F)
        svg.drawLine((lifeline.startX + root.translateX).toInt(), (lifeline.startY + root.translateY).toInt(), (lifeline.endX + root.translateX).toInt(), (lifeline.endY + root.translateY).toInt())
        svg.stroke = BasicStroke(PNConfiguration.lineWidth)
        svg.drawString(instanceName.text, labelCoordinates.x.toInt(), labelCoordinates.y.toInt())
        svg.drawLine(labelCoordinates.x.toInt(), labelCoordinates.y.toInt() + 2, labelCoordinates.x.toInt() + svg.fontMetrics.stringWidth(instanceName.text), labelCoordinates.y.toInt() + 2);

        spans.forEach { it.exportSVG(svg, root.translateX, root.translateY) }
    }
}