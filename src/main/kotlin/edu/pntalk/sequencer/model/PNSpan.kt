/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import org.apache.batik.svggen.SVGGraphics2D
import java.awt.BasicStroke

class PNSpan(val start: Double, val end: Double, val instanceName: String, val clsName: String, val diagram: DiagramController) {
    val rect : Rectangle = Rectangle(PNConfiguration.spanSize, end - start)
    init {
        rect.translateX = PNConfiguration.instanceSize.x/2 - PNConfiguration.spanSize/2
        rect.translateY = start + PNConfiguration.instanceSize.y/2
        rect.fill = Color.WHITE
        rect.stroke = Color.BLACK

        rect.onMouseClicked = EventHandler<MouseEvent> {
            val step = diagram.getStep(start + (it.sceneY - rect.localToScene(rect.x, rect.y).y))
            diagram.code.highlightSpan(start.toInt(), instanceName, clsName)
            diagram.alert.outputMessage("Visualise ${instanceName} in step ${step} and time ${start + (it.sceneY - rect.localToScene(rect.x, rect.y).y)}")
            diagram.changelog.getState(step, instanceName, clsName)
            diagram.setHighlightedNode(rect)
        }
    }

    fun exportSVG(svg: SVGGraphics2D, offsetX : Double, offsetY: Double) {
        // Convert Javafx into SVG representation
        svg.color = java.awt.Color.WHITE
        svg.fillRect((rect.x + rect.translateX + offsetX).toInt(), (rect.y + rect.translateY + offsetY).toInt(), rect.width.toInt(), rect.height.toInt())
        svg.color = java.awt.Color.BLACK
        svg.drawRect((rect.x + rect.translateX + offsetX).toInt(), (rect.y + rect.translateY + offsetY).toInt(), rect.width.toInt(), rect.height.toInt())
    }
}