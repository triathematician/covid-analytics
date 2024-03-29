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
package tri.covid19.coda.hotspot

import javafx.beans.property.*
import javafx.event.EventTarget
import javafx.scene.control.SplitPane
import javafx.scene.control.TableView
import tornadofx.*
import tri.area.usa.UsaAreaLookup
import tri.covid19.DEATHS
import tri.covid19.coda.history.*
import tri.covid19.coda.utils.*
import tri.covid19.coda.data.CovidTimeSeriesSources

/** UI for exploring historical COVID time series data. */
class HotspotTable: SplitPane() {

    val selectedMetric = SimpleStringProperty(DEATHS).apply { addListener { _ -> updateTableData() } }
    val hotspotData = mutableListOf<HotspotInfo>().asObservable()
    val minPopulation = SimpleObjectProperty(0).apply { addListener { _ -> updateTableData() } }
    val maxPopulation = SimpleObjectProperty(Int.MAX_VALUE).apply { addListener { _ -> updateTableData() } }
    val minCount = SimpleObjectProperty(100).apply { addListener { _ -> updateTableData() } }
    val minPerCapitaCount = SimpleObjectProperty(0).apply { addListener { _ -> updateTableData() } }
    val minLastWeekCount = SimpleObjectProperty(100).apply { addListener { _ -> updateTableData() } }
    val minLastWeekPerCapitaCount = SimpleObjectProperty(20).apply { addListener { _ -> updateTableData() } }

    val regionTypes = listOf(COUNTRIES, STATES, COUNTIES, CBSA)
    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply { addListener { _ -> updateTableData() } }
    val parentRegion = SimpleStringProperty("USA").apply { addListener { _ -> updateTableData() } }

    lateinit var table: TableView<HotspotInfo>

    init {
        configPanel()
        borderpane {
            top = toolbar {
                button("Copy") { action { copyTableDataToClipboard(table) } }
            }
            center = table()
        }
        updateTableData()
    }

    /** Configuration for the table. */
    private fun EventTarget.configPanel() = form {
        fieldset("Region/Metric") {
            field("Category") {
                combobox(selectedRegionType, regionTypes)
            }
            field("Metric") {
                combobox(selectedMetric, METRIC_OPTIONS)
            }
        }
        fieldset("Filtering") {
            field("Parent Region") {
                textfield(parentRegion)
            }
            field("Min Population") {
                editablespinner(0..Int.MAX_VALUE).bind(minPopulation)
            }
            field("Max Population") {
                editablespinner(0..Int.MAX_VALUE).bind(maxPopulation)
            }
            field("Min Total") {
                editablespinner(0..100000000).bind(minCount)
            }
            field("Min (per capita)") {
                editablespinner(0..10000).bind(minPerCapitaCount)
            }
            field("Min (Last 7 Days)") {
                editablespinner(0..10000).bind(minLastWeekCount)
            }
            field("Min (per capita, Last 7 Days)") {
                editablespinner(0..10000).bind(minLastWeekPerCapitaCount)
            }
        }
    }

    /** Table. */
    private fun EventTarget.table() = scrollpane(fitToWidth = true, fitToHeight = true) {
        tableview(hotspotData) {
            readonlyColumn("Region", HotspotInfo::regionId)
            readonlyColumn("FIPS", HotspotInfo::fips)
            readonlyColumn("Population", HotspotInfo::population).cellFormatUserNumber()
            readonlyColumn("Metric", HotspotInfo::metric)

            readonlyColumn("Total", HotspotInfo::value).cellFormatUserNumber()
            readonlyColumn("(per 100k)", HotspotInfo::valuePerCapita).cellFormatUserNumber()
            readonlyColumn("Last 28", HotspotInfo::dailyChange28).cellFormatUserNumber()
            readonlyColumn("(per 100k)", HotspotInfo::dailyChange28PerCapita).cellFormatUserNumber()
            readonlyColumn("Last 7", HotspotInfo::dailyChange7).cellFormatUserNumber()
            readonlyColumn("(per 100k)", HotspotInfo::dailyChange7PerCapita).cellFormatUserNumber()
            readonlyColumn("(7days/total)", HotspotInfo::percentInLast7).cellFormatPercentage()
            readonlyColumn("(7days/28days)", HotspotInfo::percentInLast7Of28).cellFormatPercentage()

            readonlyColumn("3Day %Δ", HotspotInfo::threeDayPercentChange).cellFormatPercentage()
            readonlyColumn("7Day %Δ", HotspotInfo::sevenDayPercentChange).cellFormatPercentage()

            readonlyColumn("Trend", HotspotInfo::trendDays).cellFormatDayTrend()
            readonlyColumn("(since last extreme)", HotspotInfo::changeSinceTrendExtremum).cellFormatPercentage()

            readonlyColumn("Doubling Time", HotspotInfo::doublingTimeDays).cellFormatUserNumber()
            readonlyColumn("(last 28 days)", HotspotInfo::doublingTimeDays28).cellFormatUserNumber()
            readonlyColumn("(last 14 days)", HotspotInfo::doublingTimeDays14).cellFormatUserNumber()
            readonlyColumn("(ratio)", HotspotInfo::doublingTimeDaysRatio).cellFormatUserNumber()

            readonlyColumn("Peak 7-Day Total", HotspotInfo::peak7).cellFormatUserNumber()
            readonlyColumn("(per 100k)", HotspotInfo::peak7PerCapita).cellFormatUserNumber()
            readonlyColumn("(date)", HotspotInfo::peak7Date)
            readonlyColumn("Peak 14-Day Total", HotspotInfo::peak14).cellFormatUserNumber()
            readonlyColumn("(per 100k)", HotspotInfo::peak14PerCapita).cellFormatUserNumber()
            readonlyColumn("(date)", HotspotInfo::peak14Date)

            readonlyColumn("Severity (#)", HotspotInfo::severityByChange)
            readonlyColumn("Severity (rate)", HotspotInfo::severityByDoubling)
            readonlyColumn("Severity (total)", HotspotInfo::totalSeverity)
//            readonlyColumn("Trend", HotspotInfo::severityChange)
//            readonlyColumn("3/7% Ratio", HotspotInfo::threeSevenPercentRatio).cellFormatUserNumber()

            table = this
        }
    }

    private fun updateTableData() {
        hotspotData.setAll(data()
                .filter { parentRegion.value == null || UsaAreaLookup.area(it.areaId).parent == UsaAreaLookup.area(parentRegion.value) }
                .filter { it.lastValue >= minCount.value && it.deltas().last(0..6).sum() >= minLastWeekCount.value }
                .filter { UsaAreaLookup.area(it.areaId).population == null || it.lastValue / UsaAreaLookup.area(it.areaId).population!! * 1E5 >= minPerCapitaCount.value }
                .filter { UsaAreaLookup.area(it.areaId).population == null || it.deltas().last(0..6).sum() / UsaAreaLookup.area(it.areaId).population!! * 1E5 >= minLastWeekPerCapitaCount.value }
                .hotspotPerCapitaInfo(metric = selectedMetric.value, minPopulation = minPopulation.value, maxPopulation = maxPopulation.value))
    }

    internal fun data() = when (selectedRegionType.value) {
        COUNTRIES -> CovidTimeSeriesSources.countryData(includeGlobal = true)
        STATES -> CovidTimeSeriesSources.usStateData(includeUS = true)
        COUNTIES -> CovidTimeSeriesSources.usCountyData()
        CBSA -> CovidTimeSeriesSources.usCbsaData()
        else -> throw IllegalStateException()
    }

}
