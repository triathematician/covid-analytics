package triathematician.covid19.reports

import javafx.scene.chart.NumberAxis
import javafx.scene.layout.Priority
import tornadofx.*

class TimeSeriesReportApp: App(TimeSeriesReportAppView::class)

fun main(args: Array<String>) {
    launch<TimeSeriesReportApp>(args)
}

class TimeSeriesReportAppView: View() {

    override val root = vbox {
        splitpane {
            vgrow = Priority.ALWAYS
            drawer {
                item("Configuration", expanded = true) {
                    // TODO - choosers for metrics
                }
            }
            drawer {
                item("Time Series", expanded = true) {
                    // TODO - add configuration panel, e.g. log
                    scatterchart("Historical Data", NumberAxis().apply { label = "Day" }, NumberAxis().apply { label = "Number" }) {
                        animated = false
                    }
                }
                item("Hubbert Linearization") {
                    scatterchart("Percent Growth vs Total", NumberAxis().apply { label = "Total" },
                            NumberAxis().apply {
                                label = "Percent Growth"
                                isAutoRanging = false
                                lowerBound = 0.0
                                tickUnit = 0.05
                                upperBound = 0.3
                            }) {
                        animated = false
                    }
                }
            }
        }
        hbox {
            vgrow = Priority.NEVER
            label("")
            pane {
                hgrow = Priority.ALWAYS
            }
            label("")
        }
    }
}