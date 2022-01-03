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
package tri.covid19.coda.forecast

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Text
import tornadofx.*
import tri.covid19.reports.HotspotInfo
import tri.timeseries.*
import tri.timeseries.analytics.ExtremaSummary
import tri.timeseries.analytics.ExtremeInfo
import tri.timeseries.analytics.ExtremeType
import tri.timeseries.analytics.MinMaxFinder
import tri.util.minus
import tri.util.monthDay
import tri.util.percentFormat
import tri.util.userFormat
import kotlin.math.absoluteValue

/** Panel that shows information about an underlying [TimeSeries]. */
class TimeSeriesInfoPanel(val series: SimpleObjectProperty<TimeSeries?>) : View() {

    private val popText = SimpleStringProperty("")
    private val peakText = SimpleStringProperty("")
    private val doublingText = SimpleStringProperty("")
    private val recentChangeText = SimpleStringProperty("")
    private val currentTrendText = SimpleStringProperty("")
    private val currentTrendColor = SimpleObjectProperty<Paint>(Color.BLACK)
    private val dataInfo = observableListOf<Text>()

    init {
        series.onChange { update(it) }
    }

    override val root = scrollpane {
        form {
            fieldset("Data Characteristics") {
                field("Population") { label(popText) }
                field("Peak") { label(peakText) }
                field("Doubling Time") { label(doublingText) }
                field("Recent Change") { label(recentChangeText) }
                field("Current Trend") {
                    label(currentTrendText) {
                        textFillProperty().bind(currentTrendColor)
                    }
                }
                field("History") {
                    labelContainer.alignment = Pos.TOP_LEFT
                    textflow { bindChildren(dataInfo) { it } }
                }
            }
        }
    }

    fun update(s: TimeSeries?) {
        if (s == null) {
            popText.value = ""
            peakText.value = ""
            doublingText.value = ""
            recentChangeText.value = ""
            currentTrendText.value = ""
            dataInfo.setAll()
            return
        }

        popText.value = s.area.population?.userFormat() ?: "unknown"

        val deltas = s.deltas()
        val smoothedDeltas = deltas.movingAverage(7)
        val peak = deltas.peak()
        val peakSmoothed = smoothedDeltas.peak()
        peakText.value = "${peak.second.userFormat()} on ${peak.first.monthDay}, ${peakSmoothed.second.userFormat()} on ${peakSmoothed.first.monthDay} (smoothed)"

        val hotspotInfo = HotspotInfo(s)
        doublingText.value = "${hotspotInfo.doublingTimeDays?.userFormat() ?: "N/A"} days (all time), ${hotspotInfo.doublingTimeDays28?.userFormat() ?: "N/A"} days (last 28 days)"
        recentChangeText.value = "${hotspotInfo.threeDayPercentChange?.percentFormat() ?: "N/A"} (3 day change), ${hotspotInfo.sevenDayPercentChange?.percentFormat() ?: "N/A"} (7 day change)"

        val extrema = s.deltas().extrema()
        currentTrendText.value = extrema.currentTrendText()
        currentTrendColor.value = when {
            "▲" in currentTrendText.value -> Color.DARKRED
            "▼" in currentTrendText.value -> Color.DARKGREEN
            else -> Color.BLACK
        }
        dataInfo.setAll(extrema.textInfo())
    }

    private fun TimeSeries.extrema() = MinMaxFinder(10).invoke(restrictNumberOfStartingZerosTo(1).movingAverage(7))

    //region EXTREMA TEXT

    /** Determine last extremum either at least 14 days from current, or where current value is at least 20% deviation, report on trend from that. */
    private fun ExtremaSummary.currentTrendText(): String {
        val curValue = extrema.values.last().value
        val curDate = extrema.keys.last()
        val anchorDate = extrema.keys.reversed().firstOrNull { curDate.minus(it) >= 14 ||
                curDate.minus(it) >= 7 && extrema[it]!!.value.percentChangeTo(curValue).absoluteValue >= .1 ||
                extrema[it]!!.value.percentChangeTo(curValue).absoluteValue >= .2 } ?: return ""
        val anchorValue = extrema[anchorDate]!!.value
        return when {
            curValue < anchorValue -> "▼ ${curDate.minus(anchorDate)} days     ${(curValue - anchorValue).userFormat()} since last peak (${anchorValue.percentChangeTo(curValue).percentFormat()})"
            curValue > anchorValue -> "▲ ${curDate.minus(anchorDate)} days     +${(curValue - anchorValue).userFormat()} since last valley (+${anchorValue.percentChangeTo(curValue).percentFormat()})"
            curValue == anchorValue -> "${curDate.minus(anchorDate)} days stable"
            else -> ""
        }
    }

    private fun ExtremaSummary.textInfo(): List<Text> {
        val extremaValues = extrema.values.toList()
        val globalMin = extremaValues.map { it.value }.minOrNull()!!
        val globalMax = extremaValues.map { it.value }.maxOrNull()!!
        return extremaValues.mapIndexed { i, cur -> cur.text(extremaValues.getOrNull(i - 1), globalMin, globalMax, i == extremaValues.size - 1) }
                .flatMap { it + Text(System.lineSeparator()) }
    }

    private fun ExtremeInfo.text(last: ExtremeInfo?, globalMin: Double, globalMax: Double, isLast: Boolean): List<Text> {
        if (last == null) {
            return listOf(Text("${date.monthDay}:".padStart(6) + "     first data point at ${value.userFormat()}"))
        } else if (value == last.value) {
            return listOf(Text("   ~ ${date.minus(last.date)} days at ${value.userFormat()}").apply { fill = Color.DARKGRAY })
        } else if (isLast && date.minus(last.date) <= 2L) {
            val text1 = "Currently at ${value.userFormat()}"
            val text2 = when {
                value == globalMin -> " (global min)"
                value == globalMax -> " (global max)"
                else -> ""
            }
            return listOf(Text(text1), Text(text2).apply { fill = Color.BLUE })
        }

        val increasing = value > last.value
        val text1 = "   ${if (increasing) "▲" else "▼"} ${date.minus(last.date)} days"
        val text2 = "${date.monthDay}:".padStart(6) + "\t${value.userFormat()}"
        val text3 = when (type) {
            ExtremeType.LOCAL_MIN, ExtremeType.GLOBAL_MIN -> minString(this, globalMin)
            ExtremeType.LOCAL_MAX, ExtremeType.GLOBAL_MAX -> maxString(this, globalMax)
            ExtremeType.ENDPOINT -> ""
        }
        val lastReferenceText = when (last.type) {
            ExtremeType.ENDPOINT -> "first data point"
            ExtremeType.LOCAL_MAX, ExtremeType.GLOBAL_MAX -> "last peak"
            ExtremeType.LOCAL_MIN, ExtremeType.GLOBAL_MIN -> "last valley"
        }
        val text4 = when {
            increasing -> "+${(value - last.value).userFormat()} since $lastReferenceText (+${last.value.percentChangeTo(value).percentFormat()})"
            else -> "${(value - last.value).userFormat()} since $lastReferenceText (${last.value.percentChangeTo(value).percentFormat()})"
        }

        return listOf(text1, System.lineSeparator(), "$text2 ", if (text3.isNotEmpty()) "($text3)" else "", "\t$text4")
                .map {
                    Text(it).apply {
                        when {
                            "global" in it -> fill = Color.BLUE
                            "▲" in it -> fill = Color.DARKRED
                            "▼" in it -> fill = Color.DARKGREEN
                        }
                    }
                }
    }

    private fun minString(v: ExtremeInfo, global: Double) = if (v.value == global) "global min" else if (v.type == ExtremeType.ENDPOINT) "" else "local min"
    private fun maxString(v: ExtremeInfo, global: Double) = if (v.value == global) "global max" else if (v.type == ExtremeType.ENDPOINT) "" else "local max"

    private fun Double.percentChangeTo(count: Double) = (count - this) / this

    //endregion

}
