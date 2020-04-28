package tri.covid19.reports

import tri.covid19.CovidRiskLevel

data class HotspotInfo(var region: String, var fips: String, var metric: String, var value: Number,
                       var dailyChange: Double, var doublingTimeDays: Double,
                       var severityByChange: CovidRiskLevel, var severityByDoubling: CovidRiskLevel, var severityChange: Int) {

    val totalSeverity
        get() = severityByChange.level + severityByDoubling.level

    fun toList() = listOf(region, fips, metric, value,
            dailyChange, dailyChange/value.toDouble(), doublingTimeDays,
            severityByChange.level, severityByDoubling.level, totalSeverity, severityChange)

    companion object {
        val header = listOf("location", "fips", "metric", "value",
                "change (avg)", "growth rate", "doubling time (days)",
                "severity (change)", "severity (growth)", "severity", "severity (trend)")
    }
}