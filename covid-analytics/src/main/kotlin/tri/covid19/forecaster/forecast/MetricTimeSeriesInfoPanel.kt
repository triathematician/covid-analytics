package tri.covid19.forecaster.forecast

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.text.Text
import tornadofx.*
import tri.covid19.reports.HotspotInfo
import tri.timeseries.ExtremaInfo
import tri.timeseries.ExtremaType
import tri.timeseries.MetricTimeSeries
import tri.timeseries.MinMaxFinder
import tri.util.monthDay
import tri.util.percentFormat
import tri.util.userFormat

/** Panel that shows information about an underlying [MetricTimeSeries]. */
class MetricTimeSeriesInfoPanel(val series: SimpleObjectProperty<MetricTimeSeries?>) : View() {

    private val popText = SimpleStringProperty("")
    private val peakText = SimpleStringProperty("")
    private val doublingText = SimpleStringProperty("")
    private val recentChangeText = SimpleStringProperty("")
    private val dataInfo = observableListOf<Text>()

    init {
        series.onChange { update(it) }
    }

    override val root = form {
        fieldset("Data Characteristics") {
            field("Population") { label(popText) }
            field("Peak") { label(peakText) }
            field("Doubling Time") { label(doublingText) }
            field("Recent Change") { label(recentChangeText) }
            field("History") { textflow { bindChildren(dataInfo) { it } } }
        }
    }

    fun update(s: MetricTimeSeries?) {
        if (s == null) {
            popText.value = ""
            peakText.value = ""
            doublingText.value = ""
            recentChangeText.value = ""
            dataInfo.setAll()
            return
        }

        popText.value = s.region.population?.toString() ?: "unknown"

        val deltas = s.deltas()
        val smoothedDeltas = deltas.movingAverage(7)
        val peak = deltas.peak()
        val peakSmoothed = smoothedDeltas.peak()
        peakText.value = "${peak.second.userFormat()} on ${peak.first.monthDay}, ${peakSmoothed.second.userFormat()} on ${peakSmoothed.first.monthDay} (smoothed)"

        val hotspotInfo = s.let { HotspotInfo(it.region, it.metric, it.values) }
        doublingText.value = "${hotspotInfo.doublingTimeDays.userFormat()} days (all time), ${hotspotInfo.doublingTimeDays28.userFormat()} days (last 28 days)"
        recentChangeText.value = "${hotspotInfo.threeDayPercentChange?.percentFormat() ?: "N/A"} (3 day change), ${hotspotInfo.sevenDayPercentChange?.percentFormat() ?: "N/A"} (7 day change)"

        dataInfo.setAll(s.dataInfo())
    }

    private fun MetricTimeSeries.dataInfo(): List<Text> {
        val seriesForExtrema = deltas().restrictNumberOfStartingZerosTo(1).movingAverage(7)
        val summary = MinMaxFinder(7).invoke(seriesForExtrema)
        val extremaValues = summary.extrema.values.toList()
        val globalMin = extremaValues.map { it.count }.min()!!
        val globalMax = extremaValues.map { it.count }.max()!!
        return extremaValues.mapIndexed { i, cur -> extremaText(cur, extremaValues.getOrNull(i - 1), globalMin, globalMax) }
                .flatMap { listOf(Text(it), Text(System.lineSeparator())) }
    }

    private fun extremaText(current: ExtremaInfo, last: ExtremaInfo?, globalMin: Double, globalMax: Double): String {
        val res = current.date.monthDay + ": " + current.count.userFormat()
        val change = when {
            last == null -> ""
            current.count < last.count -> "${minString(current, globalMin)} ▼${(last.count - current.count).userFormat()} (${last.count.percentChangeTo(current.count).percentFormat()})"
            current.count > last.count -> "${maxString(current, globalMax)} ▲${(current.count - last.count).userFormat()} (+${last.count.percentChangeTo(current.count).percentFormat()})"
            else -> ""
        }
        return listOf(res, change).joinToString(" ")
    }

    private fun minString(v: ExtremaInfo, global: Double) = if (v.count == global) "(MIN)" else if (v.type == ExtremaType.ENDPOINT) "" else "(min)"
    private fun maxString(v: ExtremaInfo, global: Double) = if (v.count == global) "(MAX)" else if (v.type == ExtremaType.ENDPOINT) "" else "(max)"

    private fun Double.percentChangeTo(count: Double) = (count - this) / this

}
