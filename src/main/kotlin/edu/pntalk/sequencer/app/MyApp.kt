package edu.pntalk.sequencer.app

import edu.pntalk.sequencer.model.CodeController
import edu.pntalk.sequencer.model.ProjectController
import edu.pntalk.sequencer.view.MainView
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consumeEach
import tornadofx.App

class MyApp: App(MainView::class, Styles::class) {
    //val model: MainModel by inject()
    val code: CodeController by inject()
    val project: ProjectController by inject()
    //val executor = Executors.newSingleThreadExecutor()

    override fun start(stage: Stage) {
        super.start(stage)
        stage.isMaximized = true
        code.subscription()
    }

    override fun stop() {
        super.stop()
        code.executor.shutdown()
    }
}
