package tri.covid19.reports

import tri.area.AreaInfo
import tri.area.Lookup
import tri.covid19.CovidRiskLevel
import tri.covid19.risk_DoublingTime
import tri.covid19.risk_PerCapitaDeathsPerDay
import tri.timeseries.*
import tri.util.minus
import java.time.LocalDate
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.sign

/** Aggregates information about a single hotspot associated with a region. */
data class HotspotInfo(var areaId: String, var metric: String, var start: LocalDate, var values: List<Double>, val averageDays: Int = 7) {

    constructor(series: MetricTimeSeries): this(series.areaId, series.metric, series.start, series.values)

    val area = Lookup.areaOrNull(areaId)!!

    val deltas = values.deltas()
    val deltaAverages = deltas.movingAverage(averageDays)
    val doublings = values.movingAverage(averageDays).doublingTimes()
    val doublings14 = values.movingAverage(averageDays).doublingTimes(sinceDaysAgo = 14)
    val doublings28 = values.movingAverage(averageDays).doublingTimes(sinceDaysAgo = 28)

//    val minPrior = priorInfo.mapNotNull { it?.totalSeverity }.min()
//    val maxPrior = priorInfo.mapNotNull { it?.totalSeverity }.max()
//    val severityChange = when {
//        minPrior == null || maxPrior == null -> 0
//        minPrior < riskTotal -> riskTotal - minPrior
//        else -> riskTotal - maxPrior
//    }

    val value
        get() = values.last()
    val valuePerCapita
        get() = population?.let { value/it * 1E5 }

    val dailyChange
        get() = deltaAverages.lastOrNull()
    val dailyChange7
        get() = deltaAverages.lastOrNull()?.times(7)
    val dailyChange28
        get() = deltas.movingAverage(28).lastOrNull()?.times(28)
    val percentInLast7
        get() = dailyChange7?.div(value)
    val percentInLast7Of28
        get() = dailyChange7.divideOrNull(dailyChange28)
    val doublingTimeDays
        get() = doublings.lastOrNull()
    val doublingTimeDays14
        get() = doublings14.lastOrNull()
    val doublingTimeDays28
        get() = doublings28.lastOrNull()
    val doublingTimeDaysRatio
        get() = doublingTimeDays?.divideOrNull(doublingTimeDays28)
    val severityByChange
        get() = dailyChange?.let { risk_PerCapitaDeathsPerDay(it) } ?: CovidRiskLevel.MINOR
    val severityByDoubling
        get() = doublingTimeDays?.let { risk_DoublingTime(it) } ?: CovidRiskLevel.MINOR
    val totalSeverity
        get() = severityByChange.level + severityByDoubling.level

    val perCapitaPop
        get() = population?.toDouble() divideOrNull 1E5

    val dailyChangePerCapita
        get() = dailyChange divideOrNull perCapitaPop
    val dailyChange7PerCapita
        get() = dailyChange7 divideOrNull perCapitaPop
    val dailyChange28PerCapita
        get() = dailyChange28 divideOrNull perCapitaPop

    val peak7
        get() = values.deltas().movingAverage(7).max()?.times(7) ?: 0.0
    val peak7PerCapita
        get() = peak7 divideOrNull perCapitaPop
    val peak7Date
        get() = values.deltas().movingAverage(7).withIndex().maxBy { it.value }?.index?.let { start.plusDays(it + 7L) }
    val peak14
        get() = values.deltas().movingAverage(14).max()?.times(14) ?: 0.0
    val peak14PerCapita
        get() = peak14 divideOrNull perCapitaPop
    val peak14Date
        get() = values.deltas().movingAverage(14).withIndex().maxBy { it.value }?.index?.let { start.plusDays(it + 7L) }

    val regionId
        get() = area.id
    val fips
        get() = area.fips
    val population
        get() = area.population

    private val currentTrend
        get() = MinMaxFinder(10).invoke(MetricTimeSeries(areaId, "", false, 0.0, LocalDate.now(), deltas)
                .restrictNumberOfStartingZerosTo(1).movingAverage(7))
                .let { CurrentTrend(it.extrema) }

    val trendDays
        get() = currentTrend.daysSigned
    val changeSinceTrendExtremum
        get() = currentTrend.percentChangeSinceExtremum

    val threeDayPercentChange
        get() = deltas.percentIncrease(4..6, 1..3)
    val sevenDayPercentChange
        get() = deltas.percentIncrease(8..14, 1..7)
    val threeSevenPercentRatio
        get() = threeDayPercentChange divideOrNull sevenDayPercentChange

    private fun List<Double>.percentIncrease(bottom: IntRange, top: IntRange): Double? {
        if (top.last > size || bottom.last > size) {
            return null
        }
        val first = subList(maxOf(size - bottom.last - 1, 0), maxOf(size - bottom.first, 0)).average()
        val second = subList(maxOf(size - top.last - 1, 0), maxOf(size - top.first, 0)).average()
        return (second - first)/first
    }
}

private infix fun Double?.divideOrNull(y: Double?) = when {
    this == null || y == null -> null
    else -> this/y
}

private fun Double.percentChangeTo(count: Double) = (count - this) / this

private class CurrentTrend(map: SortedMap<LocalDate, ExtremaInfo>) {
    val curValue by lazy { map.values.last().value }
    val curDate by lazy { map.keys.last()
    }
    val anchorDate by lazy {
        map.keys.reversed().firstOrNull { curDate.minus(it) >= 14 ||
            curDate.minus(it) >= 7 && map[it]!!.value.percentChangeTo(curValue).absoluteValue >= .1 ||
            map[it]!!.value.percentChangeTo(curValue).absoluteValue >= .2 } ?: map.keys.first()
    }
    val anchorValue by lazy { map[anchorDate]!!.value }
    val daysSigned by lazy { ((curValue - anchorValue).sign * curDate.minus(anchorDate)).toInt() }
    val percentChangeSinceExtremum by lazy { anchorValue.percentChangeTo(curValue) }
}
