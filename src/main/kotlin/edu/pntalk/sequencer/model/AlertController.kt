/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import edu.pntalk.sequencer.view.ErrorFragment
import javafx.scene.control.TextField
import javafx.scene.layout.Background
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import tornadofx.*
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Controller class providing logic around alerting box.
 *
 * Gathers logs from all parts, that have meaning for user.
 * Dividing them by priority.
 *
 * @property alertLog GUI element in bottom of the screen.
 * @constructor Creates an empty [alertLog] and place it.
 */
class AlertController: Controller() {
    val alertLog: VBox = VBox(-10.0)

    init {
        alertLog.useMaxWidth = true
        alertLog.fitToParentWidth()
        //alertLog.prefWidth = 2000.0
        alertLog.prefHeight = 200.0
    }

    /**
     * Format and adds a [outputMessage] to [alertLog].
     * @param outputMessage text
     */
    fun outputMessage(outputMessage: String) {
        val message = TextField("${DateTimeFormatter.ISO_INSTANT.format(Instant.now())} $outputMessage\n")
        message.isEditable = false
        message.background = Background.EMPTY
        message.isFocusTraversable = false
        message.useMaxWidth = true
        message.prefWidth = message.text.length * 7.0
        alertLog.add(message)
        message.toFront()
    }

    /**
     * Format and adds a [infoMessage] to [alertLog].
     * @param infoMessage text
     */
    fun infoMessage(infoMessage: String) {
        val message = TextField("${DateTimeFormatter.ISO_INSTANT.format(Instant.now())} $infoMessage\n")
        message.style = "-fx-text-fill: blue"
        message.isEditable = false
        message.background = Background.EMPTY
        message.isFocusTraversable = false
        message.useMaxWidth = true
        message.prefWidth = message.text.length * 7.0
        alertLog.add(message)
    }

    /**
     * Format and adds a [warningMessage] to [alertLog].
     * @param warningCode warning identifier
     * @param warningMessage text
     */
    fun warningMessage(warningCode: Int, warningMessage: String) {
        val message = TextField("${DateTimeFormatter.ISO_INSTANT.format(Instant.now())} WARNING(${warningCode}): $warningMessage\n")
        message.style = "-fx-text-fill: orange"
        message.isEditable = false
        message.background = Background.EMPTY
        message.isFocusTraversable = false
        message.useMaxWidth = true
        message.prefWidth = message.text.length * 7.0
        alertLog.add(message)
    }

    /**
     * Format and adds a [errorMessage] to [alertLog].
     * @param errorCode warning identifier
     * @param errorMessage text
     */
    fun errorMessage(errorCode: Int, errorMessage: String) {
        ErrorFragment(errorCode, errorMessage).openWindow()
        val message = TextField("${DateTimeFormatter.ISO_INSTANT.format(Instant.now())} ERROR(${errorCode}): $errorMessage\n")
        message.style = "-fx-text-fill: red"
        message.isEditable = false
        message.background = Background.EMPTY
        message.isFocusTraversable = false
        message.useMaxWidth = true
        message.prefWidth = message.text.length * 7.0
        alertLog.add(message)
    }
}