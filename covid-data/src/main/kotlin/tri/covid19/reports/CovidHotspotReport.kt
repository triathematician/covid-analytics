package tri.covid19.reports

import tri.covid19.DEATHS
import tri.timeseries.*

/** Compute hotspots of given metric. */
fun List<MetricTimeSeries>.hotspotPerCapitaInfo(metric: String = DEATHS,
                                                minPopulation: Int = 50000,
                                                valueFilter: (Double) -> Boolean = { it >= 5 }): List<HotspotInfo> {
    return filter { it.region.population?.let { it > minPopulation } ?: true }
            .filter { it.metric == metric && valueFilter(it.lastValue) }
            .map { HotspotInfo(it.region, it.metric, it.start, it.values) }
}