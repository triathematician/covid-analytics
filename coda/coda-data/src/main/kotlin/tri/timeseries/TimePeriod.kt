package tri.timeseries

/** Time periods across which [TimeSeries] data can be aggregated. */
enum class TimePeriod {
    CUMULATIVE,
    DAILY,
    WEEKLY,
    WEEKLY_TOTAL,
    WEEKLY_AVERAGE,
    MONTHLY,
    MONTHLY_TOTAL,
    MONTHLY_AVERAGE;
}