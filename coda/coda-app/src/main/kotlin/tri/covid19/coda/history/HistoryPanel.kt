/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
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
package tri.covid19.coda.history

import javafx.event.EventTarget
import javafx.scene.control.SplitPane
import tornadofx.*
import tri.covid19.coda.utils.*
import tri.util.userFormat

/** UI for exploring historical COVID time series data. */
class HistoryPanel : SplitPane() {

    private val historyPanelModel = HistoryPanelModel()
    private val hubbertChartModel = HistoryHubbertModel()

    init {
        configPanel()
        this += HistoryPanelPlots(historyPanelModel, hubbertChartModel)
    }

    /** Main configuration panel. */
    private fun EventTarget.configPanel() = form {
        fieldset("Regions") {
            field("Region Category") {
                combobox(historyPanelModel.selectedRegionType, historyPanelModel.regionTypes)
            }
            field("# of Regions") {
                editablespinner(0..200).bind(historyPanelModel._regionLimit)
                label("skip")
                editablespinner(0..200).bind(historyPanelModel._skipFirst)
            }
            field("Parent Region") {
                textfield().bind(historyPanelModel.parentRegion)
            }
            field("Include Regions") {
                checkbox().bind(historyPanelModel.includeRegionActive)
                textfield().bind(historyPanelModel.includeRegion)
            }
            field("Exclude Regions") {
                checkbox().bind(historyPanelModel.excludeRegionActive)
                textfield().bind(historyPanelModel.excludeRegion)
            }
            field("Min Population") {
                editablespinner(0..Int.MAX_VALUE).bind(historyPanelModel._minPopulation)
            }
            field("Max Population") {
                editablespinner(0..Int.MAX_VALUE).bind(historyPanelModel._maxPopulation)
            }
        }

        fieldset("Metric") {
            field("Metric") {
                combobox(historyPanelModel._selectedMetric, METRIC_OPTIONS)
                checkbox("per capita").bind(historyPanelModel._perCapita)
                checkbox("per day").bind(historyPanelModel._perDay)
            }
            field("Smooth (days)") {
                editablespinner(1..28).bind(historyPanelModel._smooth)
                checkbox("log scale").bind(historyPanelModel._logScale)
            }
            field("Extra smooth") { checkbox("extra smooth").bind(historyPanelModel._extraSmooth) }
            field("Sort") {
                val group = togglegroup {
                    selectedValueProperty<TimeSeriesSort>().value = TimeSeriesSort.ALL
                    historyPanelModel._sort.bind(selectedValueProperty())
                }
                vbox {
                    hbox {
                        spacing = 5.0
                        radiobutton("total", group, TimeSeriesSort.ALL) { isSelected = true }
                        radiobutton("last 14 days", group, TimeSeriesSort.LAST14)
                        radiobutton("last 7 days", group, TimeSeriesSort.LAST7)
                    }
                    hbox {
                        spacing = 5.0
                        radiobutton("peak incidence (7 days)", group, TimeSeriesSort.PEAK7)
                        radiobutton("peak incidence (14 days)", group, TimeSeriesSort.PEAK14)
                    }
                    hbox {
                        spacing = 5.0
                        radiobutton("population", group, TimeSeriesSort.POPULATION)
                    }
                }
            }
        }

        fieldset("Growth Plot") {
            field("Peak Curve") {
                checkbox("show").bind(hubbertChartModel.showPeakCurve)
                slider(-2.0..8.0) {
                    blockIncrement = 0.01
                    enableWhen(hubbertChartModel.showPeakCurve)
                }.bind(hubbertChartModel.logPeakValueProperty)
                label(hubbertChartModel.peakValue.userFormat()) {
                    hubbertChartModel.logPeakValueProperty.onChange { text = hubbertChartModel.peakValue.userFormat() }
                }
            }
        }
    }

}
