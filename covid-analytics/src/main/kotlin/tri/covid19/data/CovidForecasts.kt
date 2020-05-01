package tri.covid19.data

import tri.timeseries.Forecast
import kotlin.time.ExperimentalTime

/** Access to forecasts by model and date. */
@ExperimentalTime
object CovidForecasts {

    val allForecasts: List<Forecast> by lazy { ihmeForecasts + lanlForecasts }

    val ihmeForecasts: List<Forecast>
        get() = loadTimeSeries("../data/normalized/ihme-forecasts.json").flatMap { regionData ->
            regionData.metrics.groupBy { IhmeForecasts.forecastId(regionData.region.id, it.id) }.map {
                Forecast(it.key, it.value)
            }
        }

    val lanlForecasts: List<Forecast>
        get() = loadTimeSeries("../data/normalized/lanl-forecasts.json").flatMap { regionData ->
            regionData.metrics.groupBy { LanlForecasts.forecastId(regionData.region.id, it.id) }.map {
                Forecast(it.key, it.value)
            }
        }
}

