package triathematician.covid19.sources

import triathematician.timeseries.MetricTimeSeries
import triathematician.util.CsvLineSplitter
import triathematician.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FORMAT1 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Loads IHME prediction models. */
object IhmePredictions {
    /** All predictions. */
    val allPredictions by lazy { loadPredictions() }

    /** Load predictions from IHME files. */
    private fun loadPredictions(): List<MetricTimeSeries> {
        val file1 = IhmePredictions::class.java.getResource("resources/ihme-4-12.csv")
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
                    MetricTimeSeries(it.region, "", "Deaths (ihme mean)", 0.0, it.date, it.dailyDeaths.mean),
                    MetricTimeSeries(it.region, "", "Deaths (ihme lower)", 0.0, it.date, it.dailyDeaths.lower),
                    MetricTimeSeries(it.region, "", "Deaths (ihme upper)", 0.0, it.date, it.dailyDeaths.upper)
            )
        }
    }
}

private class IhmeDatum(val region: String, val date: LocalDate, val dailyDeaths: UncertaintyRange, val totalDeaths: UncertaintyRange) {
    val isProjection: Boolean
        get() = totalDeaths.lower != totalDeaths.upper
}

private class UncertaintyRange(val mean: Double, val lower: Double, val upper: Double) {
    constructor(mean: String?, lower: String?, upper: String?) : this(mean!!.toDouble(), lower!!.toDouble(), upper!!.toDouble())
}