package edu.pntalk.sequencer.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.observableList
import tornadofx.setValue

class Place(name : String, value : String) {
    val nameProperty = SimpleStringProperty(name)
    var name by nameProperty

    /*val typeProperty = SimpleIntegerProperty(type)
    var type by typeProperty*/

    val valueProperty = SimpleStringProperty(value)
    var value by valueProperty
}

class SeparateValue(val value: String, val type: Int)

class SeparatePlace(val name : String, val values : MutableList<SeparateValue>) {
    fun addValue(value : SeparateValue) {
        values.add(value)
    }
}

class ChangelogController : Controller(){
    val diagram : DiagramController by inject()
    val table : ObservableList<Place> = observableList()

    fun getTableData(separated : List<SeparatePlace>) : List<Place> {
        val result = mutableListOf<Place>()
        separated.forEach {place ->
            val str = mutableListOf<String>()
            place.values.forEach { str.add(it.value) }
            val place = Place(place.name, str.joinToString(", ") )
            result.add(place)
        }
        return result
    }

    fun getSeparateValues(archive : List<ArchiveValue>) : List<SeparateValue> {
        val result = mutableListOf<SeparateValue>()
        archive.forEach { value ->
            result.add(SeparateValue(value.value, value.type))
        }
        return result
    }

    fun getMessageState(instance: String, cls: String) {

    }

    fun getState(time : Int, instance: String, cls: String) {
        diagram.getObject(instance)?.let { obj ->
            obj.initPlaces(diagram.archive!!.initial.findLast { it.instance == obj.name }!!.places)
            val changelog = mutableListOf<SeparatePlace>()
            changelog.addAll(obj.startPlaces)

            for ((index, step) in diagram.archive!!.steps.withIndex()) {
                if (index >= time)
                    break

                val idList = mutableListOf<Int>()

                step.transition_starts.filter { it.instance == instance }.forEach {
                    idList.add(it.id)
                }

                step.transition_ends.filter { idList.contains(it.id) }.forEach { end ->
                    for (change in end.changelog) {
                        for (place in change.keys) {
                            changelog.find { it.name == place }?.values?.let { values ->
                                values.clear()
                                values.addAll(getSeparateValues(change[place]!!))
                            }
                        }
                    }
                }
            }
            table.clear()
            table.addAll(getTableData(changelog))
        }
    }
}