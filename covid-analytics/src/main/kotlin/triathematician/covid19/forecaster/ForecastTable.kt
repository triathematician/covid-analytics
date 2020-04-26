package triathematician.covid19.forecaster

import javafx.scene.layout.BorderPane
import tornadofx.*
import triathematician.util.format
import triathematician.util.userFormat

/** Panel for managing saved forecasts. */
class ForecastTable(model: ForecastPanelModel) : BorderPane() {
    init {
        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            tableview(model.userForecasts) {
                readonlyColumn("Region", UserForecast::region)
                readonlyColumn("Model", UserForecast::model)
                readonlyColumn("Metric", UserForecast::metric)
                readonlyColumn("Forecast Date", UserForecast::forecastDate)

                readonlyColumn("Peak Day", UserForecast::peakDay)
                readonlyColumn("Peak Value", UserForecast::peakValue).cellFormat { text = it?.userFormat() }
                readonlyColumn("May 1", UserForecast::may1).cellFormat { text = it?.userFormat() }
                readonlyColumn("June 1", UserForecast::june1).cellFormat { text = it?.userFormat() }
                readonlyColumn("July 1", UserForecast::july1).cellFormat { text = it?.userFormat() }
                readonlyColumn("May 1 Total", UserForecast::may1Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("June 1 Total", UserForecast::june1Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("July 1 Total", UserForecast::july1Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("Total", UserForecast::totalValue).cellFormat { text = it?.userFormat() }

                readonlyColumn("Curve", UserForecast::sigmoidCurve)
                readonlyColumn("Parameters", UserForecast::parameters).cellFormat { text = it?.joinToString("; ") { it.userFormat() } }
                readonlyColumn("k", UserForecast::parameterK).cellFormat { text = it?.format(4) }

                readonlyColumn("First Fit Day", UserForecast::fitFirstDay)
                readonlyColumn("Last Fit Day", UserForecast::fitLastDay)
                readonlyColumn("SE Totals", UserForecast::standardErrorCumulative).cellFormat { text = it?.format(2) }
                readonlyColumn("SE Deltas", UserForecast::standardErrorDelta).cellFormat { text = it?.format(2) }

                contextmenu {
                    item("Restore").action { selectedItem?.apply { model.load(this) } }
                    separator()
                    item("Remove").action { selectedItem?.apply { model.userForecasts.remove(this) } }
                }
            }
        }
    }
}