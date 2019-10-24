package edu.pntalk.sequencer

import edu.pntalk.sequencer.app.MyApp
import javafx.application.Application

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(MyApp::class.java, *args)
        }
    }
}