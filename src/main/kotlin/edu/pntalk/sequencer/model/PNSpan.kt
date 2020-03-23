package edu.pntalk.sequencer.model

import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

class PNSpan(start: Double, end: Double) {
    val rect : Rectangle = Rectangle(PNConfiguration.spanSize, end - start)
    init {
        rect.translateX = PNConfiguration.instanceSize.x/2 - PNConfiguration.spanSize/2
        rect.translateY = start + PNConfiguration.instanceSize.y/2
        rect.fill = Color.WHITE
        rect.stroke = Color.BLACK
    }
}