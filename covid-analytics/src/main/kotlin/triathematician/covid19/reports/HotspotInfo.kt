package triathematician.covid19.reports

import triathematician.covid19.CovidRiskLevel

data class HotspotInfo(var id: String, var metric: String, var value: Number,
                       var dailyChange: Double, var doublingTimeDays: Double,
                       var severityByChange: CovidRiskLevel, var severityByDoubling: CovidRiskLevel, var severityChange: Int) {

    val totalSeverity
        get() = severityByChange.level + severityByDoubling.level

    fun toList() = listOf(id, metric, value,
            dailyChange, dailyChange/value.toDouble(), doublingTimeDays,
            severityByChange.level, severityByDoubling.level, totalSeverity, severityChange)

    companion object {
        val header = listOf("location", "metric", "value",
                "change (avg)", "growth rate", "doubling time (days)",
                "severity (change)", "severity (growth)", "severity", "severity (trend)")
    }
}