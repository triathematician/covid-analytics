package tri.covid19.forecaster.hotspot

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import tornadofx.*
import tri.covid19.DEATHS
import tri.covid19.forecaster.history.*
import tri.covid19.forecaster.utils.cellFormatDayTrend
import tri.covid19.forecaster.utils.cellFormatPercentage
import tri.covid19.forecaster.utils.cellFormatUserNumber
import tri.covid19.forecaster.utils.editablespinner
import tri.covid19.reports.HotspotInfo
import tri.covid19.reports.hotspotPerCapitaInfo
import tri.util.userFormat
import triathematician.covid19.CovidTimeSeriesSources
import java.text.NumberFormat
import kotlin.time.ExperimentalTime

/** UI for exploring historical COVID time series data. */
@ExperimentalTime
class HotspotTable: SplitPane() {

    val selectedMetric = SimpleStringProperty(DEATHS).apply { addListener { _ -> updateTableData() } }
    val hotspotData = mutableListOf<HotspotInfo>().asObservable()
    val minCount = SimpleObjectProperty(100).apply { addListener { _ -> updateTableData() } }
    val minPerCapitaCount = SimpleObjectProperty(0).apply { addListener { _ -> updateTableData() } }
    val minLastWeekCount = SimpleObjectProperty(100).apply { addListener { _ -> updateTableData() } }
    val minLastWeekPerCapitaCount = SimpleObjectProperty(20).apply { addListener { _ -> updateTableData() } }

    val regionTypes = listOf(COUNTRIES, STATES, COUNTIES, CBSA)
    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply { addListener { _ -> updateTableData() } }

    init {
        configPanel()
        table()
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
            field("Min Total") {
                editablespinner(0..10000).bind(minCount)
            }
            field("Min (per capitas)") {
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
            readonlyColumn("Latest", HotspotInfo::dailyChange).cellFormatUserNumber()
            readonlyColumn("(per 100k)", HotspotInfo::dailyChangePerCapita).cellFormatUserNumber()
            readonlyColumn("Trend", HotspotInfo::trendDays).cellFormatDayTrend()
            readonlyColumn("(since last extreme)", HotspotInfo::changeSinceTrendExtremum).cellFormatPercentage()
            readonlyColumn("Doubling Time", HotspotInfo::doublingTimeDays).cellFormatUserNumber()
            readonlyColumn("(last 30 days)", HotspotInfo::doublingTimeDays28).cellFormatUserNumber()
            readonlyColumn("Severity (#)", HotspotInfo::severityByChange)
            readonlyColumn("Severity (rate)", HotspotInfo::severityByDoubling)
            readonlyColumn("Severity (total)", HotspotInfo::totalSeverity)
            readonlyColumn("Trend", HotspotInfo::severityChange)
            readonlyColumn("3Day %", HotspotInfo::threeDayPercentChange).cellFormatPercentage()
            readonlyColumn("7Day %", HotspotInfo::sevenDayPercentChange).cellFormatPercentage()
            readonlyColumn("3/7% Ratio", HotspotInfo::threeSevenPercentRatio).cellFormatUserNumber()
        }
    }

    private fun updateTableData() {
        hotspotData.setAll(data()
                .filter { it.lastValue >= minCount.value }
                .filter { it.last(1..7).average() >= minLastWeekCount.value }
                .filter { it.region.population == null || it.lastValue / it.region.population!! * 1E5 >= minPerCapitaCount.value }
                .filter { it.region.population == null || it.last(1..7).average() / it.region.population!! * 1E5 >= minLastWeekPerCapitaCount.value }
                .hotspotPerCapitaInfo(metric = selectedMetric.value, minPopulation = 0))
    }

    internal fun data() = when (selectedRegionType.value) {
        COUNTRIES -> CovidTimeSeriesSources.countryData(includeGlobal = true)
        STATES -> CovidTimeSeriesSources.usStateData(includeUS = true)
        COUNTIES -> CovidTimeSeriesSources.usCountyData()
        CBSA -> CovidTimeSeriesSources.usCbsaData()
        else -> throw IllegalStateException()
    }

}