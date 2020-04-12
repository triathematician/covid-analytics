package triathematician.pop.model.ui

import triathematician.pop.model.run
import triathematician.pop.model.impl.seir
import javafx.application.Platform
import javafx.event.EventTarget
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.Priority
import tornadofx.*
import java.util.*

class SirModelApp: App(SirModelAppView::class)

class InitialValues {
    var onChange: () -> Unit = {}

    var s by property(99997)
    val sProperty = getProperty(InitialValues::s).also {
        it.addListener { _ -> onChange() }
    }
    var e by property(3)
    val eProperty = getProperty(InitialValues::e).also {
        it.addListener { _ -> onChange() }
    }
    var i by property(0)
    val iProperty = getProperty(InitialValues::i).also {
        it.addListener { _ -> onChange() }
    }
    var r by property(0)
    val rProperty = getProperty(InitialValues::r).also {
        it.addListener { _ -> onChange() }
    }
}

class Rates {
    var onChange: () -> Unit = {}

    var beta: Number by property(0.25)
    val betaProperty = getProperty(Rates::beta).also {
        it.addListener { _ -> onChange() }
    }
    var gamma: Number by property(0.05)
    val gammaProperty = getProperty(Rates::gamma).also {
        it.addListener { _ -> onChange() }
    }
    var gamma2: Number by property(0.05)
    val gamma2Property = getProperty(Rates::gamma2).also {
        it.addListener { _ -> onChange() }
    }
}

class Results {
    val s = mutableListOf<XYChart.Data<Number, Number>>().asObservable()
    val e = mutableListOf<XYChart.Data<Number, Number>>().asObservable()
    val i = mutableListOf<XYChart.Data<Number, Number>>().asObservable()
    val r = mutableListOf<XYChart.Data<Number, Number>>().asObservable()
    val x = mutableListOf<XYChart.Data<Number, Number>>().asObservable()
}

class SirModelAppView: View() {

    val inits = InitialValues().apply { onChange = { updateResults() } }
    val vals = Rates().apply { onChange = { updateResults() } }
    val results = Results()

    private var updateTimer: Timer? = null

    override val root = vbox {
        menubar {
            menu("File") {
                item("New")
                separator()
                item("Quit")
            }
            menu("Edit")
            menu("Simulation") {
                item("Reset")
                item("Run 1x")
                item("Run 1000x")
            }
            menu("Help") {
                item("About")
            }
        }
        splitpane {
            vgrow = Priority.ALWAYS
            drawer {
                item("Configuration", expanded = true) {
                    configForm()
                }
            }
            drawer {
                item("Model Dynamics", expanded = true) {
                    areachart("Model Dynamics", NumberAxis().apply { label = "Day" }, NumberAxis().apply { label = "Population" }) {
                        animated = false
                        createSymbols = false
                        series("S", results.s)
                        series("E", results.e)
                        series("I", results.i)
                        series("R", results.r)
                    }
                }
                item("Linearization of Recoveries") {
                    scatterchart("Hubbert Transform", NumberAxis().apply { label = "Total" },
                            NumberAxis().apply {
                                label = "Percent Growth"
                                isAutoRanging = false
                                lowerBound = 0.0
                                tickUnit = 0.05
                                upperBound = 0.2
                            }) {
                        animated = false
                        series("X", results.x)
                    }
                }
            }
        }
        hbox {
            vgrow = Priority.NEVER
            label("Left status")
            pane {
                hgrow = Priority.ALWAYS
            }
            label("Right status")
        }
    }

    fun EventTarget.configForm() = form {
        fieldset("Initial Conditions") {
            field("Susceptible") {
                spinner(0, 100000000, 0, 1) {
                    isEditable = true
                }.bind(inits.sProperty)
            }
            field("Exposed") {
                spinner(0, null, 0, 1) {
                    isEditable = true
                }.bind(inits.eProperty)
            }
            field("Infectious") {
                spinner(0, null, 0, 1) {
                    isEditable = true
                }.bind(inits.iProperty)
            }
            field("Recovered") {
                spinner(0, null, 0, 1) {
                    isEditable = true
                }.bind(inits.rProperty)
            }
        }
        fieldset("Dynamics") {
            field("Mixture Rate") {
                slider(0.0, 1.0, 0.5) {
                    isShowTickLabels = true
                    isShowTickMarks = true
                    majorTickUnit = 0.1
                }.bind(vals.betaProperty)
            }
            field("Infection Rate") {
                slider(0.0, 1.0, 0.1) {
                    isShowTickLabels = true
                    isShowTickMarks = true
                    majorTickUnit = 0.1
                }.bind(vals.gammaProperty)
            }
            field("Recovery Rate") {
                slider(0.0, 1.0, 0.1) {
                    isShowTickLabels = true
                    isShowTickMarks = true
                    majorTickUnit = 0.1
                }.bind(vals.gamma2Property)
            }
        }
    }

    /** Trigger a model update that executes after a fixed period of inactivity. */
    fun updateResults() {
        updateTimer?.cancel()
        updateTimer = Timer()
        updateTimer!!.schedule(object : TimerTask() {
            override fun run() {
                Platform.runLater { rerunModel() }
                updateTimer = null
            }
        }, 50)
    }

    fun rerunModel() {
        val model = seir(inits.s + inits.e + inits.i + inits.r, inits.e, inits.i, inits.r, vals.beta.toDouble(), vals.gamma.toDouble(), vals.gamma2.toDouble())
        val data = model.run(500)
        Frac.reset()
        results.s.setAll(data.mapIndexed { i, ints -> XYChart.Data<Number, Number>(i, ints[0]) })
        results.e.setAll(data.mapIndexed { i, ints -> XYChart.Data<Number, Number>(i, ints[1]) })
        results.i.setAll(data.mapIndexed { i, ints -> XYChart.Data<Number, Number>(i, ints[2]) })
        results.r.setAll(data.mapIndexed { i, ints -> XYChart.Data<Number, Number>(i, ints[3]) })
        results.x.setAll(data.mapIndexed { i, ints -> XYChart.Data<Number, Number>(ints[3], Frac.percentChange(ints[3].toDouble())) })
    }

    private object Frac {
        var last = 0.0
        fun reset() { last = 0.0 }
        fun percentChange(tot: Double): Double {
            val delta = tot - last
            last = tot
            return if (tot == 0.0) 0.0 else delta/tot
        }
    }
}

fun main(args: Array<String>) {
    launch<SirModelApp>(args)
}