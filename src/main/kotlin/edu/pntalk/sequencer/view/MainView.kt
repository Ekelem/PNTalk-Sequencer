package edu.pntalk.sequencer.view

import edu.pntalk.sequencer.model.MainModel
import edu.pntalk.sequencer.model.Place
import javafx.geometry.Pos
import tornadofx.*


class MainView : View("Petri net Sequencer") {
    val model: MainModel by inject()
    override val root = form {
        menubar {
            menu("File") {
                item("Settings")
                menu("Save") {
                    item("Code")
                    item("Scenario")
                }
                item("Export SVG")
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
        splitpane() {
            tabpane {
                tab("Code") {
                    vbox {
                        textarea(model.code) {
                            textProperty().bindBidirectional(model.code)
                            setPrefSize(500.0, 600.0)
                            style = ".content{-fx-background-color: black; }"
                        }
                    }
                }
                tab("Scenario") {
                    vbox {
                        textarea(model.scenario) {
                            textProperty().bindBidirectional(model.scenario)
                            setPrefSize(500.0, 600.0)
                            style = ".content{-fx-background-color: black; }"
                        }
                    }
                }
            }
            vbox {
                button("DRAW") {
                    useMaxWidth = true
                    setOnAction {
                        runAsync {
                            model.simulate(model.steps)
                        }

                        //find<Report>().openModal(stageStyle = StageStyle.UTILITY)
                    }

                    //disableProperty().bind(!model.validCode.toProperty())
                }
                label(model.progressComment) {
                    vboxConstraints {
                        marginTop = 20.0
                        marginBottom = 10.0
                    }
                }
                progressbar(model.completion) {
                    useMaxWidth = true
                }
                checkbox("Using Scenario", model.usingScenario) {
                    vboxConstraints {
                        marginTop = 20.0
                        marginBottom = 20.0
                    }
                }
                hbox {
                    label("Simulation Steps: ")
                    textfield("5") {
                        //textProperty().bindBidirectional(model.steps.toProperty(), NumberStringConverter())
                        textProperty().addListener { obs, old, new ->
                            try {
                                model.steps = new.toInt()
                                style = "-fx-background-color: white;"
                            } catch (exception: Throwable) {
                                style = "-fx-background-color: #FFD2D2;"
                            }

                        }
                    }
                }

                borderpane {
                    vboxConstraints {
                        marginTop = 30.0
                    }
                    alignment = Pos.BOTTOM_CENTER
                    center  = tableview<Place> {
                        items = model.preview
                        .observable()

                        column("NAME",Place::name)
                        column("VALUE",Place::value)
                    }
                }
            }
            tabpane() {
                tab("Sequence  Diagram"){
                    vbox {
                        add(model.input)
                    }
                }
            }
        }
    }
}

class Report: Fragment() {
    override val root = label("Report")
}
