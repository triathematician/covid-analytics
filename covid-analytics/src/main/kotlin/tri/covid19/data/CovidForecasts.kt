package tri.covid19.data

import tri.timeseries.Forecast
import kotlin.time.ExperimentalTime

/** Access to forecasts by model and date. */
@ExperimentalTime
object CovidForecasts {

    val FORECAST_OPTIONS = listOf(IHME, LANL, YYG)

    val allForecasts: List<Forecast> by lazy { ihmeForecasts + lanlForecasts + yygForecasts }

    val ihmeForecasts: List<Forecast>
        get() = loadTimeSeries("../data/normalized/ihme-forecasts.json").flatMap { regionData ->
            regionData.metrics.groupBy { IhmeForecasts.forecastId(regionData.region, it.id) }.map {
                Forecast(it.key, it.value)
            }
        }

    val lanlForecasts: List<Forecast>
        get() = loadTimeSeries("../data/normalized/lanl-forecasts.json").flatMap { regionData ->
            regionData.metrics.groupBy { LanlForecasts.forecastId(regionData.region, it.id) }.map {
                Forecast(it.key, it.value)
            }
        }

    val yygForecasts: List<Forecast>
        get() = loadTimeSeries("../data/normalized/yyg-forecasts.json").flatMap { regionData ->
            regionData.metrics.groupBy { YygForecasts.forecastId(regionData.region, it.id) }.map {
                Forecast(it.key, it.value)
            }
        }
}

