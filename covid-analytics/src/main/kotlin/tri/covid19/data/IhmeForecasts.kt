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

const val IHME = "IHME"

private val FORECAST_DATES = listOf("4-01", "4-12", "4-20")
private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

/** Loads IHME forecast data. */
object IhmeForecasts {

    val forecasts: List<Forecast> by lazy { loadForecasts() }

    /** Load predictions from IHME files. */
    private fun loadForecasts(): List<Forecast> = FORECAST_DATES.flatMap { date ->
        val dataLines = IhmeForecasts::class.java.getResource("resources/ihme-$date.csv").csvKeyValues()
                .map {
                    IhmeDatum(it["location_name"]!!, it["date"]!!.toLocalDate(),
                            UncertaintyRange(it["deaths_mean"], it["deaths_lower"], it["deaths_upper"]),
                            UncertaintyRange(it["totdea_mean"], it["totdea_lower"], it["totdea_upper"]))
                }.toList()

        dataLines.groupBy { it.region }.map { (region, data) ->
            val metrics = data.flatMap { it.toMetrics(date) }.regroupAndMerge(false)
            Forecast(IHME, "2020-$date".toLocalDate(FORMAT), region, DEATHS, metrics)
        }
    }

}

private class IhmeDatum(_region: String, val date: LocalDate, val dailyDeaths: UncertaintyRange, val totalDeaths: UncertaintyRange) {
    var region: String = _region.normalizeRegionId()
    val isForecast: Boolean
        get() = totalDeaths.lower != totalDeaths.upper

    fun toMetrics(forecastDate: String): List<MetricTimeSeries> = when {
        !isForecast -> emptyList()
        else -> listOf(
//                metric("change, $IHME $forecastDate mean", dailyDeaths.mean),
//                metric("change, $IHME $forecastDate lower", dailyDeaths.lower),
//                metric("change, $IHME $forecastDate upper", dailyDeaths.upper),
                metric("$IHME $forecastDate mean", totalDeaths.mean),
                metric("$IHME $forecastDate lower", totalDeaths.lower),
                metric("$IHME $forecastDate upper", totalDeaths.upper))
    }

    private fun metric(id: String, value: Double) = MetricTimeSeries(region, "", "$DEATHS ($id)", 0.0, date, value)
}

/** Normalizes ID's by region. */
private fun String.normalizeRegionId() = when {
    UnitedStates.stateNames.contains(this) -> "$this, US"
    this == "United States of America" -> "US"
    else -> this
}