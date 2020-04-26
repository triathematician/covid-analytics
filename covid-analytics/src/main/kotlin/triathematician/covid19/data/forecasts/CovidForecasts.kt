package triathematician.covid19.data.forecasts

import triathematician.timeseries.Forecast

/** Access to forecasts by date and organization. */
object CovidForecasts {
    val allForecasts: List<Forecast> by lazy { IhmeForecasts.forecasts + LanlForecasts.forecasts }
}

