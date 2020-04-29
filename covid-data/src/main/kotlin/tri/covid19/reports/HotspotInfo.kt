package tri.covid19.reports

import tri.covid19.CovidRiskLevel
import tri.regions.RegionLookup

data class HotspotInfo(var region: String, var metric: String, var value: Double,
                       var dailyChange: Double, var doublingTimeDays: Double,
                       var severityByChange: CovidRiskLevel, var severityByDoubling: CovidRiskLevel, var severityChange: Int) {

    val regionInfo
        get() = RegionLookup(region)

    val totalSeverity
        get() = severityByChange.level + severityByDoubling.level
    val fips
        get() = regionInfo.fips
    val population
        get() = regionInfo.population

    val valuePerCapita
        get() = population?.let { value/it * 1E5 }
    val dailyChangePerCapita
        get() = population?.let { dailyChange/it * 1E5 }

    fun toList() = listOf(region, regionInfo.fips, metric, value,
            dailyChange, dailyChange/value, doublingTimeDays,
            severityByChange.level, severityByDoubling.level, totalSeverity, severityChange)

    companion object {
        val header = listOf("location", "fips", "metric", "value",
                "change (avg)", "growth rate", "doubling time (days)",
                "severity (change)", "severity (growth)", "severity", "severity (trend)")
    }
}