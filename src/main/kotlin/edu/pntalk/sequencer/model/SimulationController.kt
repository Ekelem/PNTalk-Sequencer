/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import com.google.rpc.Code
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import tornadofx.Controller
import virtualmachine.Simulate
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class SimulationController: Controller() {
    val code: CodeController by inject()
    val alert: AlertController by inject()
    val diagram: DiagramController by inject()

    //var grpcClient : GrpcClient? = null
    val usingScenario = SimpleBooleanProperty(false)
    val steps = SimpleStringProperty("5")
    val progressComment = SimpleStringProperty("Pipeline not running")
    val completion = SimpleDoubleProperty()
    val initial = SimpleStringProperty("C0")

    fun verifyLocal(semicode: File) : Boolean{
        val file = createTempFile()
        file.writeText(code.getCode())
        val proc = ProcessBuilder("./Translator2", "-f", file.name)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(30, TimeUnit.SECONDS)
        file.delete()
        val validCode = proc.exitValue() == 0
        if (validCode) {
            semicode.writeText(proc.inputStream.bufferedReader().readText())
            print("verified")
            print(semicode.readText())
            return true
        }
        else
            print(proc.inputStream.bufferedReader().readText())
        return false
    }

    fun simulateLocal() {

    }

    fun simulateNetwork() {

    }

    @UnstableDefault
    fun simulate() = runBlocking {

        if (PNConfiguration.validLocal())
        {

        }
        else {
            try {
                if (PNConfiguration.grpcClient == null)
                {
                    Platform.runLater(Runnable {
                        alert.errorMessage(Code.CANCELLED_VALUE, "Can not create gRPC client.")
                    })
                    return@runBlocking
                }
                val reply: Simulate.SimulateReply = PNConfiguration.grpcClient!!.simulate("main ${initial.value}\n" + code.getCode(), steps.value.toLong())
                if (reply.status != Code.OK_VALUE.toLong()) {
                    Platform.runLater(Runnable {
                        alert.errorMessage(Code.INVALID_ARGUMENT_VALUE, reply.result)
                    })
                    return@runBlocking
                }
                val json = Json(JsonConfiguration.Stable)
                val archive = json.parse(Archive.serializer(), reply.result)
                print(reply.result)
                diagram.draw(archive)
                Platform.runLater(Runnable {
                    alert.outputMessage("Diagram completed.")
                })
                return@runBlocking


            } catch (e : StatusException) {
                Platform.runLater(Runnable {
                    alert.errorMessage(
                            Code.UNAVAILABLE_VALUE,
                            "Service unavailable on ${PNConfiguration.networkSimulatorHost.value}:${PNConfiguration.networkSimulatorPort.value}")
                    alert.infoMessage("Try running simulation locally or double check your server host address.")
                })
                return@runBlocking
            }
        }
        completion.set(0.1)
        Platform.runLater {
            progressComment.set("Verifying PNTalk code")
        }
        val semicode: File = createTempFile()
        try {
            if (!verifyLocal(semicode)) {
                print("translator Error")
                return@runBlocking
            }
        }
        catch (e: IOException) {
            print(e)
        }

        completion.set(0.6)
        Platform.runLater {
            progressComment.set("Computing Simulation steps..")
        }
        //sfile.writeText(scenario.get())

        val proc = startVirtualMachineLocal(semicode)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        val json = Json.nonstrict
        proc.waitFor(60, TimeUnit.SECONDS)

        completion.set(0.8)
        Platform.runLater {
            progressComment.set("Decoding Simulation..")
        }
        val jsonString = proc.inputStream.bufferedReader().readText()
        completion.set(0.9)
        Platform.runLater {
            progressComment.set("Generating Sequence Diagram..")
        }
        val archive = json.parse(Archive.serializer(), jsonString)
        diagram.draw(archive)
        completion.set(1.0)
        Platform.runLater {
            progressComment.set("Done.")
        }
    }

    fun startVirtualMachineLocal(semicode: File): ProcessBuilder {
        return if (usingScenario.get()) {
            ProcessBuilder("./VM2", "-f", semicode.path,"-c","scenario.yaml","-s", steps.toString())
        }
        else {
            ProcessBuilder("./VM2", "-f","output.btw", "-s", steps.toString())
        }
    }
}