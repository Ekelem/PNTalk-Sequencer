package edu.pntalk.sequencer.view

import edu.pntalk.sequencer.model.*
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.stage.FileChooser
import kotlinx.coroutines.coroutineScope
import tornadofx.*
import java.io.File


class MainView : View("Petri net Sequencer") {
    val code: CodeController by inject()
    val project: ProjectController by inject()
    val simulator: SimulationController by inject()
    val diagram: DiagramController by inject()
    val changelog: ChangelogController by inject()
    val alert: AlertController by inject()
    val export: ExportController by inject()

    override val root = form {
        menubar {
            menu("File") {
                item("Open Project", "Shortcut+O") {
                    action {
                        project.open()
                    }
                }
                item("Settings") {
                    action {
                        openInternalWindow<Settings>()
                    }
                }
                item("Save", "Shortcut+S") {
                    action {
                        project.save()
                    }
                }
                item("Export SVG") {
                    action {
                        export.exportSVG()
                    }
                }
                item("Quit")
            }
            menu("Edit") {
                item("Copy")
                item("Paste")
            }
            menu("Help") {
                item("Documentation")
                item("Examples")
            }
        }
        vbox {
            useMaxWidth = true
            splitpane {
                tabpane {
                    this.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                    tab("Code") {
                        vbox {
                            hbox {
                                project.updateTreeView()
                                add(project.treeview)
                                add(code.codeArea)
                            }

                            /*textarea(model.code) {
                            textProperty().bindBidirectional(model.code)
                            setPrefSize(500.0, 600.0)
                            style = ".content{-fx-background-color: black; }"
                        }*/
                        }
                    }
                    tab("Scenario") {
                        vbox {
                            add(code.scenarioArea) /*{
                                //textProperty().bindBidirectional(code.scenarioArea.te)
                                setPrefSize(500.0, 600.0)
                                style = ".content{-fx-background-color: black; }"
                            }*/
                        }
                    }
                }
                vbox {
                    minWidth = 150.0
                    button("DRAW") {
                        useMaxWidth = true
                        setOnAction {
                            runAsync {
                                simulator.simulate()
                            }

                            //find<Report>().openModal(stageStyle = StageStyle.UTILITY)
                        }

                        //disableProperty().bind(!model.validCode.toProperty())
                    }
                    label(simulator.progressComment) {
                        vboxConstraints {
                            marginTop = 20.0
                            marginBottom = 10.0
                        }
                    }
                    progressbar(simulator.completion) {
                        useMaxWidth = true
                    }
                    checkbox("Using Scenario", simulator.usingScenario) {
                        vboxConstraints {
                            marginTop = 20.0
                            marginBottom = 20.0
                        }
                    }
                    hbox {
                        label("Simulation Steps: ") {
                            prefWidth = 200.0
                        }
                        textfield("5") {
                            textProperty().addListener { obs, old, new ->
                                try {
                                    simulator.steps.value = new.toInt().toString()
                                    style = "-fx-background-color: white;"
                                } catch (exception: Throwable) {
                                    style = "-fx-background-color: #FFD2D2;"
                                }
                            }
                        }
                    }

                    hbox {
                        label("Main Class: ") {
                            prefWidth = 200.0
                        }
                        textfield("C0") {

                            textProperty().addListener { obs, old, new ->
                                simulator.initial.value = new.toString()
                                if (new.toString().contains(Regex("\\s+"))) {
                                    style = "-fx-background-color: #FFD2D2;"
                                }
                                else {
                                    style = "-fx-background-color: white;"
                                }
                            }
                        }
                    }

                    borderpane {
                        vboxConstraints {
                            marginTop = 30.0
                        }
                        alignment = Pos.BOTTOM_CENTER
                        center = tableview<Place> {
                            items = changelog.table

                            column("NAME", Place::name)
                            column("VALUE", Place::value)
                        }
                    }
                }
                tabpane {
                    this.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                    tab("Sequence  Diagram") {
                        vbox {
                            add(diagram.input)
                        }
                    }
                }
            }
            scrollpane {
                useMaxWidth = true
                prefHeight = 200.0
                maxHeight = 200.0
                style = "-fx-background-color: #363636;"
                add(alert.alertLog)
            }

        }
    }
}

class Report: Fragment() {
    override val root = label("Report")
}

class ExportNothing: Fragment() {
    override val root = vbox {
        label("Error")
        text("Have nothing to Export")
    }
}

class NewFileFragment(private val path: String, private val suffix: String, private val project: ProjectController): Fragment() {
    override val root = vbox {
        label("Choose name: ")
        val name = textfield("file")
        button("Ok") {
            action {
                val file = File(path + name.text + suffix)
                file.writeText(project.generateNewClassCode(name.text))
                close()
                project.treeview.refresh()
            }
        }
    }
}

class NewDirFragment(private val path: String, private val project: ProjectController): Fragment() {
    override val root = vbox {
        label("Choose new Directory name: ")
        val name = textfield("Directory")
        button("Ok") {
            action {
                val file = File(path + name.text)
                file.mkdirs()
                project.updateTreeView()
                close()
            }
        }
    }
}

class ErrorFragment(private val errorCode : Int, private val errorMessage : String): Fragment() {
    override val root = vbox {
        label("Error ${errorCode}")
        text(errorMessage)
    }
}

class Settings: Fragment() {
    override val root =
        form {
            hbox {
                spacing = 30.0
                field {  }
                button("...") {
                    action {
                        var dir = chooseDirectory("Select Target Directory")
                    }
                }
            }
        }
}