package edu.pntalk.sequencer.model

import javafx.stage.FileChooser
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import org.w3c.dom.Document
import tornadofx.Controller
import java.awt.BasicStroke
import java.io.File

class ExportController: Controller() {
    val diagram: DiagramController by inject()
    val alert: AlertController by inject()

    fun exportSVG() {
        if (!diagram.isValid())
        {
            alert.errorMessage(16, "No Diagram to export.")
            return
        }

        val fileChooser = FileChooser()
        fileChooser.title = "Save as"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter(
                "Save as",
                "svg"))
        val file = fileChooser.showSaveDialog(null)

        val domImpl = GenericDOMImplementation.getDOMImplementation()
        // Create an instance of org.w3c.dom.Document.
        val svgNS = "http://www.w3.org/2000/svg"
        val document: Document = domImpl.createDocument(svgNS, "svg", null)

        val svgGenerator = SVGGraphics2D(document)
        //svgGenerator.stroke = BasicStroke(0.3F)
        //svgGenerator.drawLine(0, 1, 8, 1)
        diagram.getMessages().forEach { it.exportSVG(svgGenerator) }
        diagram.getObjects().forEach { it.exportSVG(svgGenerator) }

        val useCSS = true // we want to use CSS style attributes
        svgGenerator.stream(file.path, useCSS)
        alert.infoMessage("Exported to: ${file.path}")
    }
}