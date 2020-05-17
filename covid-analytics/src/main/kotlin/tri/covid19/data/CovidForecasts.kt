package tri.covid19.data

import tri.covid19.data.LanlForecasts.forecastId
import tri.timeseries.Forecast
import java.io.File
import kotlin.time.ExperimentalTime

/** Access to forecasts by model and date. */
@ExperimentalTime
object CovidForecasts {

    val FORECAST_OPTIONS = listOf(IHME, LANL, YYG)

    fun modelColor(name: String) = when {
        IHME in name -> "008000"
        LANL in LANL -> "4682b4"
        YYG in name -> "b44682"
        else -> "808080"
    }

    val allForecasts: List<Forecast> by lazy { loadForecasts() }

    private fun loadForecasts(): List<Forecast> {
        return File("../data/normalized/").walk().filter { it.name.endsWith("-forecasts.json") }.toList()
                .flatMap { fileForecasts(it) }
    }

    private fun fileForecasts(file: File): List<Forecast> {
        val model = file.nameWithoutExtension.substringBefore("-").toUpperCase()
        return loadTimeSeries(file).flatMap { regionData ->
            regionData.metrics.groupBy { forecastId(model, regionData.region, it.id) }
                    .filter { it.key != null }
                    .map { Forecast(it.key!!, it.value) }
        }
    }

}

