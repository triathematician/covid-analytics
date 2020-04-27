package tri.timeseries

import java.time.LocalDate

/** A single forecast, with model/forecast date, targeted region/metric, and associated time series with forecast data. */
data class Forecast(val model: String, val forecastDate: LocalDate, val region: String, val metric: String, val data: List<MetricTimeSeries>)