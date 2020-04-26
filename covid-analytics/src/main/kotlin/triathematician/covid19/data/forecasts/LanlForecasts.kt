package triathematician.covid19.data.forecasts

import triathematician.covid19.DEATHS
import triathematician.population.UnitedStates
import triathematician.timeseries.Forecast
import triathematician.timeseries.MetricTimeSeries
import triathematician.timeseries.UncertaintyRange
import triathematician.timeseries.regroupAndMerge
import triathematician.util.csvKeyValues
import triathematician.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val LANL = "LANL"

private val FORECAST_DATES = listOf("4-12", "4-22")
private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

/** Loads LANL models. */
object LanlForecasts {

    val forecasts: List<Forecast> by lazy { loadForecasts() }

    /** Load predictions from LANL files. */
    private fun loadForecasts(): List<Forecast> = FORECAST_DATES.flatMap { date ->
        val dataLines = IhmeForecasts::class.java.getResource("resources/lanl-$date.csv").csvKeyValues()
                .map {
                    LanlDatum(it["state"]!!, it["dates"]!!.toLocalDate(),
                            UncertaintyRange(it["q.50"], it["q.025"], it["q.975"]), it["obs"] == "0")
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