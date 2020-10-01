package tri.timeseries

import tri.area.Lookup
import java.time.LocalDate

/** A single forecast, with model/forecast date, targeted region/metric, and associated time series with forecast data. */
data class Forecast(val model: String, val forecastDate: LocalDate, val areaId: String, val metric: String, val data: List<MetricTimeSeries>) {
    constructor(id: ForecastId, data: List<MetricInfo>)
            : this(id.model, id.forecastDate, id.areaId, id.metric, data.map { it.toMetricTimeSeries(id.areaId) })

    val area = Lookup.areaOrNull(areaId)!!
}

/** Unique tuple describing a forecast. */
data class ForecastId(val model: String, val forecastDate: LocalDate, val areaId: String, val metric: String)