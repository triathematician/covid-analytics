package tri.covid19.reports

import tri.covid19.risk_DoublingTime
import tri.covid19.risk_PerCapitaDeathsPerDay
import tri.timeseries.*
import tri.util.minus
import java.time.LocalDate
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.sign

/** Aggregates information about a single hotspot associated with a region. */
data class HotspotInfo(var region: RegionInfo, var metric: String, var values: List<Double>, val averageDays: Int = 7) {

    val deltas = values.deltas()
    val deltaAverages = deltas.movingAverage(averageDays)
    val doublings = values.movingAverage(averageDays).doublingTimes()
    val doublings28 = values.movingAverage(averageDays).doublingTimes(sinceDaysAgo = 28)

//    val minPrior = priorInfo.mapNotNull { it?.totalSeverity }.min()
//    val maxPrior = priorInfo.mapNotNull { it?.totalSeverity }.max()
//    val severityChange = when {
//        minPrior == null || maxPrior == null -> 0
//        minPrior < riskTotal -> riskTotal - minPrior
//        else -> riskTotal - maxPrior
//    }

    val dailyChange
        get() = deltaAverages.last()
    val doublingTimeDays
        get() = doublings.last()
    val doublingTimeDays28
        get() = doublings28.last()
    val severityByChange
        get() = risk_PerCapitaDeathsPerDay(dailyChange)
    val severityByDoubling
        get() = risk_DoublingTime(doublingTimeDays)
    val totalSeverity
        get() = severityByChange.level + severityByDoubling.level
    val severityChange
        get() = 0

    val regionId
        get() = region.id
    val fips
        get() = region.fips
    val population
        get() = region.population

    val value
        get() = values.last()

    val valuePerCapita
        get() = population?.let { value/it * 1E5 }
    val dailyChangePerCapita
        get() = population?.let { dailyChange/it * 1E5 }

    private val currentTrend
        get() = MinMaxFinder(10).invoke(MetricTimeSeries(RegionInfo("", RegionType.UNKNOWN, ""), "", false, 0.0, LocalDate.now(), deltas)
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
        val first = subList(size - bottom.last - 1, size - bottom.first).average()
        val second = subList(size - top.last - 1, size - top.first).average()
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
        map.keys.reversed().first { curDate.minus(it) >= 14 ||
            curDate.minus(it) >= 7 && map[it]!!.value.percentChangeTo(curValue).absoluteValue >= .1 ||
            map[it]!!.value.percentChangeTo(curValue).absoluteValue >= .2 }
    }
    val anchorValue by lazy { map[anchorDate]!!.value }
    val daysSigned by lazy { ((curValue - anchorValue).sign * curDate.minus(anchorDate)).toInt() }
    val percentChangeSinceExtremum by lazy { anchorValue.percentChangeTo(curValue) }
}
