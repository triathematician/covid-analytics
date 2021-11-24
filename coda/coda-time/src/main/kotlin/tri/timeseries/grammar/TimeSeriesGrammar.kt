package tri.timeseries.grammar

/**
 * Grammar for [TimeSeries], allowing time series calculations to be saved, restored, and shared. Grammars are defined
 * by an extract step (where to get the [TimeSeries]), a transform step (intermediate operations), and a load step
 * (final operations).
 */
class TimeSeriesGrammar {
    val extract = listOf<TimeSeriesSource>()
    val transform = listOf<TimeSeriesOperation>()
    val load = listOf<TimeSeriesLoader>()

    /** Performs the extract, transform, and load steps prescribed by the grammar. */
    fun calculate() : List<TimeSeries> {
        var series = extract.flatMap { it.extract() }.associateBy { it.id }
        transform.forEach { series = it.transform(series) }
        return load.flatMap { it.load(series) }
    }
}

/** Provides a unique id for a time series. */
interface TimeSeriesUid

/** Index for time series, by id. */
typealias TimeSeriesIndex = Map<TimeSeriesUid, TimeSeries>

/** Abstraction of a time series. */
interface TimeSeries {
    val id: TimeSeriesUid
}

/** Provides time series data. */
interface TimeSeriesSource {
    fun extract(): List<TimeSeries>
}

/** Performs an operation or series of operation on given data elements. */
interface TimeSeriesOperation {
    fun transform(series: TimeSeriesIndex): TimeSeriesIndex
}

/** Finalizes a list of time series from the provided collection of data. */
interface TimeSeriesLoader {
    fun load(series: TimeSeriesIndex): List<TimeSeries>
}