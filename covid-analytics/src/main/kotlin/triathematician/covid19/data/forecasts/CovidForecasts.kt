package triathematician.covid19.data.forecasts

import triathematician.timeseries.MetricTimeSeries
import java.time.LocalDate

/** Access to forecasts by date and organization. */
object CovidForecasts {
    val allForecasts: List<Forecast> by lazy { IhmeForecasts.forecasts + LanlForecasts.forecasts }
}

/** A single forecast, with model/forecast date, targeted region/metric, and associated time series with forecast data. */
data class Forecast(val model: String, val forecastDate: LocalDate, val region: String, val metric: String, val data: List<MetricTimeSeries>)