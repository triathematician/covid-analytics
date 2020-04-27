package tri.covid19.data

import tri.timeseries.Forecast

/** Access to forecasts by date and organization. */
object CovidForecasts {
    val allForecasts: List<Forecast> by lazy { IhmeForecasts.forecasts + LanlForecasts.forecasts }
}

