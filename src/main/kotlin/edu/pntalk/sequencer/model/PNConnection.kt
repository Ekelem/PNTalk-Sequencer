/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.input.MouseEvent
import javafx.scene.shape.Line
import javafx.scene.text.Text
import org.apache.batik.svggen.SVGGraphics2D
import tornadofx.addChildIfPossible
import java.awt.BasicStroke
import java.awt.Stroke
import kotlin.math.absoluteValue

class PNConnection(name : String, caller : PNObject, val receiver: PNObject, val time : Double, val diagram : DiagramController, val response: Boolean = false) {
    val root : Group = Group()
    val connection : Line = Line(caller.x + caller.rect.width/2 +
            (if (caller.x - receiver.x < 0) PNConfiguration.spanSize/2 else -PNConfiguration.spanSize/2),
            caller.rect.height + time,
            receiver.x + receiver.rect.width/2 +
                    (if (caller.x - receiver.x > 0) if (name == "<<create>>") receiver.rect.width/2 else PNConfiguration.spanSize/2
                    else if (name == "<<create>>") -receiver.rect.width/2 else -PNConfiguration.spanSize/2),
            receiver.rect.height + time)
    val arrowUpwing : Line = Line(connection.endX, receiver.rect.height + time,
            connection.endX +
                    (if (caller.x - receiver.x > 0) 16.0 else -16.0),
            receiver.rect.height + time - 8.0)
    val arrowDownwing : Line = Line(connection.endX, receiver.rect.height + time,
            connection.endX +
                    (if (caller.x - receiver.x > 0) 16.0 else -16.0),
            receiver.rect.height + time + 8.0)

    var hasReply : Boolean = false
    val caller : PNObject = caller
    val messageName : Text = Text(((caller.x + receiver.x) / 2) + caller.rect.width/2, receiver.rect.height + time - 8.0, name)
    init {
        messageName.x = (messageName.x - messageName.layoutBounds.width/2)
        root.translateY = PNConfiguration.offset.y
        if (response)
            connection.strokeDashArray.addAll(20.0, 10.0)
        root.addChildIfPossible(connection)
        root.addChildIfPossible(messageName)
        root.addChildIfPossible(arrowUpwing)
        root.addChildIfPossible(arrowDownwing)

        root.onMouseClicked = EventHandler<MouseEvent> {
            diagram.code.highlightConnection(messageName.text, receiver.classname, caller.classname)
            diagram.setHighlightedNode(root)
            val step = diagram.getStep(time)
            diagram.changelog.getState(step, receiver.name, receiver.classname)
        }
    }

    fun confirmReply() {
        hasReply = true
        receiver.startSpan(time)
    }

    fun exportSVG(svg: SVGGraphics2D) {
        // Convert Javafx into SVG representation
        if (response) {
            svg.stroke = BasicStroke(PNConfiguration.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0F, floatArrayOf(9f), 0F)
            svg.drawLine((connection.startX + root.translateX).toInt(), (connection.startY + root.translateY).toInt(), (connection.endX + root.translateX).toInt(), (connection.endY + root.translateY).toInt())
            svg.stroke = BasicStroke(PNConfiguration.lineWidth)
        } else
            svg.drawLine((connection.startX + root.translateX).toInt(), (connection.startY + root.translateY).toInt(), (connection.endX + root.translateX).toInt(), (connection.endY + root.translateY).toInt())

        svg.drawLine((arrowUpwing.startX + root.translateX + arrowUpwing.translateX).toInt(), (arrowUpwing.startY + root.translateY + arrowUpwing.translateY).toInt(), (arrowUpwing.endX + root.translateX + arrowUpwing.translateX).toInt(), (arrowUpwing.endY + root.translateY + arrowUpwing.translateY).toInt())
        svg.drawLine((arrowDownwing.startX + root.translateX + arrowDownwing.translateX).toInt(), (arrowDownwing.startY + root.translateY + arrowDownwing.translateY).toInt(), (arrowDownwing.endX + root.translateX + arrowDownwing.translateX).toInt(), (arrowDownwing.endY + root.translateY + arrowDownwing.translateY).toInt())
        svg.drawString(messageName.text, (messageName.x + root.translateX).toInt(), (messageName.y + root.translateY).toInt())
    }
}