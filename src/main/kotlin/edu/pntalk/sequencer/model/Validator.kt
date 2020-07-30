package edu.pntalk.sequencer.model
/*
import kotlinx.coroutines.*
import tornadofx.*
import java.io.File
import java.util.concurrent.TimeUnit

object Validator {
    val file = File("code.txt")
    var proc : Process = ProcessBuilder("./Translator2", "-f","code.txt")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    var verifier = GlobalScope.launch {
        file.writeText("")
        if (proc.info().toString() != "[]")
            proc.destroy()
    }

    var valid : Boolean = false
    init {
        file.writeText("")
    }
    fun verify(code : String) {
        if (proc.info().toString() != "[]")
            proc.destroy()

        file.writeText(code)

        proc = ProcessBuilder("./Translator2", "-f","code.txt")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        val job = GlobalScope.launch {
            proc.waitFor(30, TimeUnit.SECONDS)
            valid = proc.exitValue() == 0
            if (valid)
                print("debug: valid")
            else
                print("debug: invalid")

        }
        verifier = job
    }
}*/