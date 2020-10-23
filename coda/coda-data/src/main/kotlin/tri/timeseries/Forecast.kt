package tri.timeseries

import tri.area.Lookup
import java.time.LocalDate

/** A single forecast, with model/forecast date, targeted region/metric, and associated time series with forecast data. */
data class Forecast(val model: String, val forecastDate: LocalDate, val areaId: String, val metric: String, val data: List<TimeSeries>) {
    val area = Lookup.areaOrNull(areaId)!!

    constructor(forecastId: ForecastId, data: List<TimeSeries>) : this(forecastId.model, forecastId.forecastDate, forecastId.areaId, forecastId.metric, data)
}

/** Unique tuple describing a forecast. */
data class ForecastId(val model: String, val forecastDate: LocalDate, val areaId: String, val metric: String)