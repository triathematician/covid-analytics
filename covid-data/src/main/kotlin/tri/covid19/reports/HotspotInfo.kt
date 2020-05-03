package tri.covid19.reports

import tri.covid19.CovidRiskLevel
import tri.regions.RegionLookup
import tri.timeseries.RegionInfo

/** Aggregates information about a single hotspot associated with a region. */
data class HotspotInfo(var region: RegionInfo, var metric: String, var value: Double,
                       var dailyChange: Double, var doublingTimeDays: Double,
                       var severityByChange: CovidRiskLevel, var severityByDoubling: CovidRiskLevel, var severityChange: Int) {

    val totalSeverity
        get() = severityByChange.level + severityByDoubling.level
    val regionId
        get() = region.id
    val fips
        get() = region.fips
    val population
        get() = region.population

    val valuePerCapita
        get() = population?.let { value/it * 1E5 }
    val dailyChangePerCapita
        get() = population?.let { dailyChange/it * 1E5 }

    fun toList() = listOf(region, region.fips, metric, value,
            dailyChange, dailyChange/value, doublingTimeDays,
            severityByChange.level, severityByDoubling.level, totalSeverity, severityChange)

    companion object {
        val header = listOf("location", "fips", "metric", "value",
                "change (avg)", "growth rate", "doubling time (days)",
                "severity (change)", "severity (growth)", "severity", "severity (trend)")
    }
}