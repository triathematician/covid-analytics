package tri.covid19.reports

import tri.covid19.DEATHS
import tri.covid19.risk_DoublingTime
import tri.covid19.risk_PerCapitaDeathsPerDay
import tri.timeseries.*

/** Compute hotspots of given metric. */
fun List<MetricTimeSeries>.hotspotPerCapitaInfo(metric: String = DEATHS,
                                                minPopulation: Int = 50000,
                                                valueFilter: (Double) -> Boolean = { it >= 5 }): List<HotspotInfo> {
    return filter { it.region.population?.let { it > minPopulation } ?: true }
            .filter { it.metric == metric && valueFilter(it.lastValue) }
            .filter { it.values.movingAverage(5).doublingTimes().lastOrNull()?.isFinite() ?: false }
            .flatMap { it.hotspotPerCapitaInfo() }
}

/** Compute hotspot info given time series data. Uses average changes over the last N days. */
fun MetricTimeSeries.hotspotPerCapitaInfo(averageDays: Int = 7, includePriorDays: Boolean = false): List<HotspotInfo> {
    val changes = values.deltas().movingAverage(averageDays)
    val doublings = values.movingAverage(averageDays).doublingTimes()
    val doublings30 = values.movingAverage(averageDays).doublingTimes(day0 = 30)

    val minSize = minOf(changes.size, doublings.size, values.size)
    val info3 = if (minSize >= 3) hotspotPerCapitaInfo(region, "$metric -2", values.thirdToLast(), changes.thirdToLast(), doublings.thirdToLast(), doublings30.thirdToLast()) else null
    val info2 = if (minSize >= 2) hotspotPerCapitaInfo(region, "$metric -1", values.penultimate(), changes.penultimate(), doublings.penultimate(), doublings30.penultimate(), info3) else null
    val info = hotspotPerCapitaInfo(region, metric, values.lastOrNull(), changes.lastOrNull(), doublings.lastOrNull(), doublings30.lastOrNull(), info2, info3)

    return if (includePriorDays) listOfNotNull(info, info2, info3) else listOfNotNull(info)
}

/** Compute hotspot info given values and information about the last few days to use when computing trends. */
private fun hotspotPerCapitaInfo(region: RegionInfo, metric: String, value: Number?, dailyChange: Double?, doublingTimeDays: Double?,
                                 doublingTimeDays30: Double?, vararg priorInfo: HotspotInfo?): HotspotInfo? {
    if (value == null || dailyChange == null || doublingTimeDays == null || doublingTimeDays30 == null) {
        return null
    }
    val severityByChange = risk_PerCapitaDeathsPerDay(dailyChange)
    val severityByDoubling = risk_DoublingTime(doublingTimeDays)
    val riskTotal =  severityByChange.level + severityByDoubling.level
    val minPrior = priorInfo.mapNotNull { it?.totalSeverity }.min()
    val maxPrior = priorInfo.mapNotNull { it?.totalSeverity }.max()
    val severityChange = when {
        minPrior == null || maxPrior == null -> 0
        minPrior < riskTotal -> riskTotal - minPrior
        else -> riskTotal - maxPrior
    }
    return HotspotInfo(region, metric, value.toDouble(), dailyChange, doublingTimeDays, doublingTimeDays30, severityByChange, severityByDoubling, severityChange)
}

private fun <X> List<X>.penultimate() = getOrNull(size - 2)
private fun <X> List<X>.thirdToLast() = getOrNull(size - 3)