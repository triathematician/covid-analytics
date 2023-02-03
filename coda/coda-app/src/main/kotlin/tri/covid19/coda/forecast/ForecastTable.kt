/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.covid19.coda.forecast

import javafx.scene.control.TableView
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import tornadofx.*
import tri.covid19.coda.utils.copyTableDataToClipboard
import tri.util.DefaultMapper
import tri.util.format
import tri.util.userFormat


/** Panel for managing saved forecasts. */
class ForecastTable(model: ForecastPanelModel) : BorderPane() {

    lateinit var table: TableView<ForecastStats>

    init {
        top = toolbar {
            button("Copy") { action { copyTableDataToClipboard(table) } }
            button("Export...") { action { exportForecastData(model.forecastInfoList) } }
        }
        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            tableview(model.forecastInfoList) {
                readonlyColumn("Region", ForecastStats::regionId)
                readonlyColumn("Model", ForecastStats::model)
                readonlyColumn("Metric", ForecastStats::metric)
                readonlyColumn("Forecast Date", ForecastStats::forecastDate)

                readonlyColumn("Peak Day", ForecastStats::peakDay)
                readonlyColumn("Peak Value", ForecastStats::peakValue).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of April", ForecastStats::apr30Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of May", ForecastStats::may31Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of June", ForecastStats::june30Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of July", ForecastStats::july31Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("May Total", ForecastStats::mayTotal).cellFormat { text = it.userFormat() }
                readonlyColumn("June Total", ForecastStats::juneTotal).cellFormat { text = it.userFormat() }
                readonlyColumn("July Total", ForecastStats::julyTotal).cellFormat { text = it.userFormat() }
                readonlyColumn("Total", ForecastStats::totalValue).cellFormat { text = it?.userFormat() }

                readonlyColumn("Curve", ForecastStats::sigmoidCurve)
                readonlyColumn("Parameters", ForecastStats::parameters).cellFormat { text = it?.joinToString(";") { it.userFormat() } }
                readonlyColumn("k", ForecastStats::parameterK).cellFormat { text = it?.format(4) }

                readonlyColumn("First Fit Day", ForecastStats::fitFirstDay)
                readonlyColumn("Last Fit Day", ForecastStats::fitLastDay)
                readonlyColumn("RMSE Totals", ForecastStats::rmsErrorCumulative).cellFormat { text = it?.format(2) }
                readonlyColumn("RMSE Deltas", ForecastStats::rmsErrorDelta).cellFormat { text = it?.format(2) }
                readonlyColumn("MASE Totals", ForecastStats::masErrorCumulative).cellFormat { text = it?.format(2) }
                readonlyColumn("MASE Deltas", ForecastStats::masErrorDelta).cellFormat { text = it?.format(2) }

                contextmenu {
                    item("Restore").action { selectedItem?.apply { model.load(this) } }
                    separator()
                    item("Remove").action { selectedItem?.apply { model.forecastInfoList.remove(this) } }
                }

                table = this
            }
        }
    }

    private fun exportForecastData(forecastStats: List<ForecastStats>) {
        val fileChooser = FileChooser()
        fileChooser.title = "Export forecast data"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("JSON Files (.json)", "*.json"))
        fileChooser.showSaveDialog(scene.window)?.run {
            DefaultMapper.writerWithDefaultPrettyPrinter().writeValue(this, forecastStats)
        }
    }

}
