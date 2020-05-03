package tri.covid19.forecaster

import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.BorderPane
import tornadofx.*
import tri.covid19.DEATHS
import tri.covid19.reports.HotspotInfo
import tri.covid19.reports.hotspotPerCapitaInfo
import tri.util.userFormat
import triathematician.covid19.CovidTimeSeriesSources
import kotlin.time.ExperimentalTime

/** UI for exploring historical COVID time series data. */
@ExperimentalTime
class HotspotTable: BorderPane() {

    val selectedMetric = SimpleStringProperty(DEATHS).apply { addListener { _ -> updateTableData() } }
    val hotspotData = mutableListOf<HotspotInfo>().asObservable()

    val regionTypes = listOf(COUNTRIES, STATES, COUNTIES, CBSA)
    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply { addListener { _ -> updateTableData() } }

    init {
        top = toolbar {
            combobox(selectedRegionType, regionTypes)
            combobox(selectedMetric, METRIC_OPTIONS)
        }
        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            tableview(hotspotData) {
                readonlyColumn("Region", HotspotInfo::regionId)
                readonlyColumn("FIPS", HotspotInfo::fips)
                readonlyColumn("Population", HotspotInfo::population)
                readonlyColumn("Metric", HotspotInfo::metric)
                readonlyColumn("Total", HotspotInfo::value).cellFormat { text = it.userFormat() }
                readonlyColumn("(per 100k)", HotspotInfo::valuePerCapita).cellFormat { text = it?.userFormat() }
                readonlyColumn("Latest", HotspotInfo::dailyChange).cellFormat { text = it.userFormat() }
                readonlyColumn("(per 100k)", HotspotInfo::dailyChangePerCapita).cellFormat { text = it?.userFormat() }
                readonlyColumn("Doubling Time", HotspotInfo::doublingTimeDays).cellFormat { text = it.userFormat() }
                readonlyColumn("Severity (#)", HotspotInfo::severityByChange)
                readonlyColumn("Severity (rate)", HotspotInfo::severityByDoubling)
                readonlyColumn("Severity (total)", HotspotInfo::totalSeverity)
                readonlyColumn("Trend", HotspotInfo::severityChange)
            }
        }
        updateTableData()
    }

    private fun updateTableData() {
        hotspotData.setAll(data().hotspotPerCapitaInfo(metric = selectedMetric.value, minPopulation = 0))
    }

    internal fun data() = when (selectedRegionType.value) {
        COUNTRIES -> CovidTimeSeriesSources.countryData(includeGlobal = true)
        STATES -> CovidTimeSeriesSources.usStateData(includeUS = true)
        COUNTIES -> CovidTimeSeriesSources.usCountyData()
        CBSA -> CovidTimeSeriesSources.usCbsaData()
        else -> throw IllegalStateException()
    }

}