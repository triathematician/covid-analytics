package triathematician.covid19.ui

import javafx.scene.layout.Priority
import tornadofx.*

class TimeSeriesReportApp : App(TimeSeriesReportAppView::class)

fun main(args: Array<String>) {
    launch<TimeSeriesReportApp>(args)
}

/** View configuration for the app. */
class TimeSeriesReportAppView : View() {

    override val root = vbox {
        drawer {
            vgrow = Priority.ALWAYS
            item("Historical Data", expanded = true) {
                this += HistoryPanel()
            }
            item("Forecasts") {
                this += ForecastPanel()
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