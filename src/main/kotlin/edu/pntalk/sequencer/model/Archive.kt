/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import kotlinx.serialization.Serializable
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern


@Serializable
data class ArchiveMessage (val id: Int = 0, val message_name: String = "", val caller_instance: String = "", val caller_class: String = "",
                            val receiver_instance: String = "", val receiver_class: String = "", val transition: String = "" ,val respond_to: Int = 0, val response:List<String> = listOf())

@Serializable
data class ArchiveTransStart (val transition_name: String = "", val instance: String = "", val instance_class: String= "", val id: Int= 0)

@Serializable
data class ArchiveTransEnd (val id: Int= 0, val changelog: List<Map<String, List<ArchiveValue>>>)

@Serializable
data class ArchiveValue (val type: Int= 0, val value: String)

@Serializable
data class ArchiveStep (val messages: List<ArchiveMessage>, val  transition_starts: List<ArchiveTransStart>, val transition_ends: List<ArchiveTransEnd>)

@Serializable
data class ArchiveInitial (val instance: String = "main", val cls: String, val creation: Int = 0, val places: List<Map<String, List<ArchiveValue>>>)

@Serializable
data class Archive (val steps: List<ArchiveStep> = listOf(), val initial: List<ArchiveInitial> = listOf())
