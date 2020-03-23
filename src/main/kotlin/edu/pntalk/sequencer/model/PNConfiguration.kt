package edu.pntalk.sequencer.model

import javafx.geometry.Point2D

object PNConfiguration {
    var offset = Point2D(200.0, 200.0)
    var instanceSize = Point2D(100.0, 40.0)
    var spanSize: Double = 20.0
    var instanceSpace: Double = 100.0

    fun setOffsetX(x: Double) {
        offset = Point2D(x, offset.y)
    }

    fun setOffsetY(y: Double) {
        offset = Point2D(offset.x, y)
    }
}