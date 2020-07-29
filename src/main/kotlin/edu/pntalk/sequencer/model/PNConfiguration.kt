package edu.pntalk.sequencer.model

import io.grpc.ManagedChannelBuilder
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Point2D
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import tornadofx.observableList
import tornadofx.onChange
import java.awt.BasicStroke
import java.awt.Color
import java.io.File


object PNConfiguration {
    var offset = Point2D(200.0, 200.0)
    var instanceSize = Point2D(100.0, 40.0)
    var spanSize: Double = 20.0
    var instanceSpace: Double = 100.0
    var lineWidth = 0.3F
    var lineDash = 7F
    private var stroke = BasicStroke(lineWidth)
    private var dashedStroke = BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0F, floatArrayOf(lineDash), 0F)
    var diagramScale = 1.0
    private var color = Color.BLACK
    val networkSimulatorHost = SimpleStringProperty("147.229.196.76")
    val networkSimulatorPort = SimpleIntegerProperty(51898)
    var grpcClient : GrpcClient? = null
    var localTranslate = false
    var localVM = false
    var lTranslator = File("Translator.exe")
    var lVM = File("VM2.exe")
    var grpcTranslator = ""
    var grpcVM = ""
    val highlightGlobal = SimpleListProperty(observableList("KEYWORD", "CLS"))
    val highlightClass = SimpleListProperty(observableList("KEYWORD", "CLS", "TRANS", "PLACE", "METHOD", "SYNC", "PAREN", "BRACKET", "BRACE"))
    val highlightShadow = DropShadow()

    fun changeGrpcAddress() {
        if (grpcClient != null) {
            grpcClient!!.close()
            grpcClient = null
        }

        grpcClient = GrpcClient(ManagedChannelBuilder.forAddress(networkSimulatorHost.value, networkSimulatorPort.value)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build())
    }

    init {
        networkSimulatorHost.onChange {
            changeGrpcAddress()
        }
        networkSimulatorPort.onChange {
            changeGrpcAddress()
        }
        highlightShadow.blurType = BlurType.GAUSSIAN
        highlightShadow.color = javafx.scene.paint.Color.GREEN
        highlightShadow.height = 15.0
        highlightShadow.width = 15.0
        highlightShadow.radius = 15.0
        networkSimulatorHost.set("147.229.196.76")
    }

    fun setOffsetX(x: Double) {
        offset = Point2D(x, offset.y)
    }

    fun setOffsetY(y: Double) {
        offset = Point2D(offset.x, y)
    }

    fun setColor() {

    }

    private fun setStroke() {
        stroke = BasicStroke(lineWidth)
    }

    private fun setDashedStroke() {
        dashedStroke = BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0F, floatArrayOf(lineDash), 0F)
    }

    fun setLocalTranslate() : Boolean {
        if (lTranslator.exists() and lTranslator.canExecute())
            localTranslate = true
        return localTranslate
    }

    fun setRemoteTranslate() : Boolean {
        localTranslate = false
        return !localTranslate
    }

    fun validLocal() : Boolean {
        return (lTranslator.exists() and lTranslator.canExecute())
    }
}