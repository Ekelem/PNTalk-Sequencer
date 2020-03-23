package edu.pntalk.sequencer.model

import javafx.scene.Group
import javafx.scene.shape.Line
import javafx.scene.text.Text
import tornadofx.*


class PNStepSpacer(id : Int, time : Double) {
    val root : Group = Group()
    val spacer : Line = Line(0.0, 60.0 + time, 50000.0, 60.0 + time)
    val upperLabel : PNLabel = PNLabel(10.0, 58.0 + time, "step $id")
    val lowerLabel : Text = Text(10.0, 72.0 + time, "step " + (id+1).toString())
    init {
        root.translateY = PNConfiguration.offset.y
        spacer.strokeDashArray.addAll(10.0, 10.0)
        spacer.strokeWidth = 0.3
        root.addChildIfPossible(spacer)
        root.addChildIfPossible(upperLabel)
        root.addChildIfPossible(lowerLabel)
    }
}

class PNLabel(x : Double, y: Double, text: String) : javafx.scene.text.Text(x, y, text) {
    override fun relocate(x: Double, y: Double) {
        val unchanched = getX()
        super.relocate(unchanched, y)
    }
}