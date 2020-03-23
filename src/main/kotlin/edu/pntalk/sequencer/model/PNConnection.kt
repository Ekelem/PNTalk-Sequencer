package edu.pntalk.sequencer.model

import javafx.scene.Group
import javafx.scene.shape.Line
import javafx.scene.text.Text
import tornadofx.*
import kotlin.math.absoluteValue

class PNConnection(name : String, caller : PNObject, receiver : PNObject, val time : Double, response: Boolean = false) {
    val root : Group = Group()
    val arrowUpwing : Line = Line(receiver.x + receiver.rect.width/2, receiver.rect.height + time,
            receiver.x + receiver.rect.width/2 + ((caller.x - receiver.x) / 16.0), receiver.rect.height + time - 8.0)
    val arrowDownwing : Line = Line(receiver.x + receiver.rect.width/2, receiver.rect.height + time,
            receiver.x + receiver.rect.width/2 + ((caller.x - receiver.x) / 16.0), receiver.rect.height + time + 8.0)
    var connection : Line = Line(caller.x + caller.rect.width/2,
            caller.rect.height + time,
            receiver.x + receiver.rect.width/2,
            receiver.rect.height + time)
    var hasReply : Boolean = false
    val caller : PNObject = caller
    val receiver: PNObject = receiver
    val messageName : Text = Text(((caller.x + receiver.x) / 2) + caller.rect.width/2, receiver.rect.height + time - 8.0, name)
    init {
        if ((receiver.creation-time).absoluteValue < 0.1)
        {
            connection.endX = receiver.x
            if (caller.x > receiver.x)
            {
                arrowDownwing.translateX = receiver.rect.width/2
                arrowUpwing.translateX = receiver.rect.width/2
            }
            else {
                arrowDownwing.translateX = -receiver.rect.width/2
                arrowUpwing.translateX = -receiver.rect.width/2
            }
        }

        messageName.x = (messageName.x - messageName.layoutBounds.width/2);
        root.translateY = PNConfiguration.offset.y
        if (response)
            connection.strokeDashArray.addAll(20.0, 10.0)
        root.addChildIfPossible(connection)
        root.addChildIfPossible(messageName)
        root.addChildIfPossible(arrowUpwing)
        root.addChildIfPossible(arrowDownwing)
    }

    fun confirmReply() {
        hasReply = true
        receiver.startSpan(time)
    }
}