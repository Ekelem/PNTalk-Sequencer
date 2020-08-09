package edu.pntalk.sequencer

import edu.pntalk.sequencer.app.Styles
import edu.pntalk.sequencer.model.CodeController
import edu.pntalk.sequencer.model.ProjectController
import edu.pntalk.sequencer.view.MainView
import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import tornadofx.App
import tornadofx.addStageIcon


class Launcher: App(MainView::class, Styles::class) {

    val code: CodeController by inject()
    val project: ProjectController by inject()
    val icon = Image("file:/img/icons8-edit.png")

    override fun start(stage: Stage) {
        super.start(stage)
        stage.icons.clear()
        stage.icons.add(icon)
        //addStageIcon(icon)
        stage.show()
        stage.isMaximized = true
        code.subscription()
    }

    override fun stop() {
        super.stop()
        code.executor.shutdown()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Launcher::class.java, *args)
        }
    }
}
