package tri.covid19.reports

import tri.covid19.DEATHS
import tri.timeseries.*

/** Compute hotspots of given metric. */
fun List<TimeSeries>.hotspotPerCapitaInfo(metric: String = DEATHS,
                                          minPopulation: Int = 50000,
                                          maxPopulation: Int = Int.MAX_VALUE,
                                          valueFilter: (Double) -> Boolean = { it >= 5 }): List<HotspotInfo> {
    return filter { it.area.population?.let { it in minPopulation..maxPopulation } ?: true }
            .filter { it.metric == metric && valueFilter(it.lastValue) }
            .map { HotspotInfo(it.areaId, it.metric, it.start, it.values) }
}