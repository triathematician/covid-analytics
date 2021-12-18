package tri.timeseries

/** Indicates an issue with an invalid time series calculation. */
class TimeSeriesDataException(message: String, cause: Throwable? = null) : Exception(message, cause)