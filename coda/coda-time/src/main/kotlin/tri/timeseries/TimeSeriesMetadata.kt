package tri.timeseries

/** Properties of the time series -- may also definite it uniquely. */
class TimeSeriesMetadata(
        /** Source for the time series. */
        var source: String,
        /** ID of the area for this time series. */
        var areaId: String,
        /** Metric reported in this time series. */
        var metric: String,
        /** Qualifier for this time series, e.g. for breakdowns by age/demographics. */
        var qualifier: String = "",
)