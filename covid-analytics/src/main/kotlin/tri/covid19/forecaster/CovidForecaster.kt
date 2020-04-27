package tri.covid19.forecaster

import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.layout.Priority
import tornadofx.*

class CovidForecaster : App(CovidForecasterView::class, CovidForecasterStyles::class)

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

class CovidForecasterStyles: Stylesheet() {
    companion object {
        val chartHover by cssclass()
    }

    init {
        chartHover {
            effect = DropShadow(BlurType.GAUSSIAN, c("dodgerblue"), 10.0, 0.2, 0.0, 0.0)
        }
    }
}