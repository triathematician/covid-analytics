package triathematician.covid19.forecaster

import javafx.scene.layout.Priority
import tornadofx.*

class CovidForecaster : App(CovidForecasterView::class)

fun main(args: Array<String>) {
    launch<CovidForecaster>(args)
}

/** View configuration for the app. */
class CovidForecasterView : View() {

    override val root = vbox {
        drawer {
            vgrow = Priority.ALWAYS
            item("Historical Data", expanded = true) {
                this += HistoryPanel()
            }
            val forecastPanel = ForecastPanel()
            item("Forecast Tool") {
                this += forecastPanel
            }
            item("Forecast Table") {
                this += ForecastTable(forecastPanel.model)
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