package tri.covid19.data

import tri.timeseries.TimeSeriesQuery
import kotlin.time.ExperimentalTime

/** Data sources made available for query by this package. */
@ExperimentalTime
object LocalCovidDataQuery : TimeSeriesQuery(JhuDailyReports, IhmeForecasts, LanlForecasts, YygForecasts) {

}