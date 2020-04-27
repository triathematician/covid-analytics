package tri.covid19.data

import tri.covid19.DEATHS
import tri.regions.UnitedStates
import tri.timeseries.Forecast
import tri.timeseries.MetricTimeSeries
import tri.timeseries.UncertaintyRange
import tri.timeseries.regroupAndMerge
import tri.util.csvKeyValues
import tri.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val LANL = "LANL"

private val FORECAST_DATES = listOf("4-05", "4-12", "4-22")
private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

/** Loads LANL models. */
object LanlForecasts {

    val forecasts: List<Forecast> by lazy { loadForecasts() }

    /** Load predictions from LANL files. */
    private fun loadForecasts(): List<Forecast> = FORECAST_DATES.flatMap { date ->
        val dataLines = IhmeForecasts::class.java.getResource("resources/lanl-$date.csv").csvKeyValues()
                .map {
                    LanlDatum(it["state"]!!, it["dates"]!!.toLocalDate(),
                            UncertaintyRange(it["q.50"], it["q.05"], it["q.95"]), it["obs"] == "0")
                }.toList()

        dataLines.groupBy { it.region }.map { (region, data) ->
            val metrics = data.flatMap { it.toMetrics(date) }.regroupAndMerge(false)
            Forecast(LANL, "2020-$date".toLocalDate(FORMAT), region, DEATHS, metrics)
        }
    }

}

private class LanlDatum(_region: String, val date: LocalDate, val totalDeaths: UncertaintyRange, val isForecast: Boolean) {
    var region: String = _region.normalizeRegionId()

    fun toMetrics(forecastDate: String): List<MetricTimeSeries> = when {
        !isForecast -> emptyList()
        else -> listOf(metric("$LANL $forecastDate mean", totalDeaths.mean),
                metric("$LANL $forecastDate lower", totalDeaths.lower),
                metric("$LANL $forecastDate upper", totalDeaths.upper))
    }

    private fun metric(id: String, value: Double) = MetricTimeSeries(region, "", "$DEATHS ($id)", 0.0, date, value)
}

/** Normalizes ID's by region. */
private fun String.normalizeRegionId() = when {
    UnitedStates.stateNames.contains(this) -> "$this, US"
    else -> this
}