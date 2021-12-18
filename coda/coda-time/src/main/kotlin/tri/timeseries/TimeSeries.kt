package tri.timeseries

import java.time.temporal.Temporal

/**
 * Time series of a single metric, which provide periodic values on some [TimePeriod], and may provide cumulative values
 * as well if supported.
 */
class TimeSeries<X, V>(
        /** Metadata for the time series: where it came from, what it describes, etc. */
        var metadata: TimeSeriesMetadata,
        /** Time period associated with the series: daily, weekly, etc. */
        var period: TimePeriod,
        /** Data associated with the series. */
        var data: TimeSeriesData<X, V>,
) where X : Temporal, X : Comparable<X>

