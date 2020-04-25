package triathematician.covid19.sources

import triathematician.population.UnitedStates
import triathematician.timeseries.MetricTimeSeries
import triathematician.timeseries.regroupAndMerge
import triathematician.util.CsvLineSplitter
import triathematician.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FORMAT1 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Loads IHME models. */
object IhmeProjections {
    /** All predictions. */
    val allProjections by lazy { loadPredictions() }

    /** Load predictions from IHME files. */
    private fun loadPredictions(): List<MetricTimeSeries> {
        val file1 = IhmeProjections::class.java.getResource("resources/ihme-4-12.csv")
        val lines = file1.readText().lines()
        val header = CsvLineSplitter.splitLine(lines[0])
        val dataLines = lines.drop(1).map { CsvLineSplitter.splitLine(it) }
                .map { datum -> datum.mapIndexed { i, s -> header[i] to s }.toMap() }
                .map {
                    IhmeDatum(it["location_name"]!!, it["date"]!!.toLocalDate(),
                            UncertaintyRange(it["deaths_mean"], it["deaths_lower"], it["deaths_upper"]),
                            UncertaintyRange(it["totdea_mean"], it["totdea_lower"], it["totdea_upper"]))
                }

        return dataLines.filter { it.isProjection }.flatMap {
            listOf(
                    MetricTimeSeries(it.region, "", "Deaths (change, ihme mean)", 0.0, it.date, it.dailyDeaths.mean),
                    MetricTimeSeries(it.region, "", "Deaths (change, ihme lower)", 0.0, it.date, it.dailyDeaths.lower),
                    MetricTimeSeries(it.region, "", "Deaths (change, ihme upper)", 0.0, it.date, it.dailyDeaths.upper),
                    MetricTimeSeries(it.region, "", "Deaths (ihme mean)", 0.0, it.date, it.totalDeaths.mean),
                    MetricTimeSeries(it.region, "", "Deaths (ihme lower)", 0.0, it.date, it.totalDeaths.lower),
                    MetricTimeSeries(it.region, "", "Deaths (ihme upper)", 0.0, it.date, it.totalDeaths.upper)
            )
        }.regroupAndMerge(false)
    }
}

private class IhmeDatum(_region: String, val date: LocalDate, val dailyDeaths: UncertaintyRange, val totalDeaths: UncertaintyRange) {
    var region: String = _region.normalizeRegionId()
    val isProjection: Boolean
        get() = totalDeaths.lower != totalDeaths.upper
}

private class UncertaintyRange(val mean: Double, val lower: Double, val upper: Double) {
    constructor(mean: String?, lower: String?, upper: String?) : this(mean!!.toDouble(), lower!!.toDouble(), upper!!.toDouble())
}

/** Normalizes ID's by region. */
private fun String.normalizeRegionId() = when {
    UnitedStates.stateNames.contains(this) -> "$this, US"
    this == "United States of America" -> "US"
    else -> this
}