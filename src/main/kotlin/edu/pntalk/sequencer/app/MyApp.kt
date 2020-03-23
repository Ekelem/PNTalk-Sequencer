package edu.pntalk.sequencer.app

import edu.pntalk.sequencer.view.MainView
import javafx.stage.Stage
import tornadofx.App

class MyApp: App(MainView::class, Styles::class) {
    override fun start(stage: Stage) {
        super.start(stage)
        stage.isMaximized = true;
    }
}