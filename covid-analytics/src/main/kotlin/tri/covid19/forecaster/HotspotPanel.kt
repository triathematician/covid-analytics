package tri.covid19.forecaster

import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.scene.control.SplitPane
import javafx.scene.control.TableView
import javafx.scene.layout.BorderPane
import tornadofx.*
import tri.covid19.reports.HotspotInfo
import tri.covid19.reports.hotspotPerCapitaInfo
import triathematician.covid19.CovidTimeSeriesSources
import java.lang.IllegalStateException
import kotlin.time.ExperimentalTime

/** UI for exploring historical COVID time series data. */
@ExperimentalTime
class HotspotPanel: BorderPane() {

    val hotspotData = mutableListOf<HotspotInfo>().asObservable()

    val regionTypes = listOf(COUNTRIES, STATES, COUNTIES)
    val selectedRegionType = SimpleStringProperty(regionTypes[1]).apply { addListener { _ -> updateTableData() } }

    init {
        top = toolbar {
            combobox(selectedRegionType, regionTypes)
        }
        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            tableview(hotspotData) {
                readonlyColumn("Region", HotspotInfo::region)
                readonlyColumn("Metric", HotspotInfo::metric)
                readonlyColumn("Latest", HotspotInfo::dailyChange)
                readonlyColumn("Doubling Time", HotspotInfo::doublingTimeDays)
                readonlyColumn("Severity (#)", HotspotInfo::severityByChange)
                readonlyColumn("Severity (rate)", HotspotInfo::severityByDoubling)
                readonlyColumn("Trend", HotspotInfo::severityChange)
            }
        }
        updateTableData()
    }

    private fun updateTableData() {
        hotspotData.setAll(data().hotspotPerCapitaInfo())
    }

    internal fun data() = when (selectedRegionType.get()) {
        COUNTRIES -> CovidTimeSeriesSources.countryData(includeGlobal = false)
        STATES -> CovidTimeSeriesSources.usStateData(includeUS = false)
        COUNTIES -> CovidTimeSeriesSources.usCountyData()
        else -> throw IllegalStateException()
    }

}