package tri.timeseries

import java.time.LocalDate
import tri.util.DateRange

/**
 * Provides various ways to aggregate date/value data into a [TimeSeries].
 */
enum class TimeSeriesAggregate(private val aggregator: (List<Pair<LocalDate, Number?>>, LocalDate?) -> Map<LocalDate, Number>) {
    /** Sums entries. */
    SUM({ pairs, date ->
        val dateValues = pairs.groupBy { it.first }.mapValues { it.value.map { it.second } }.toSortedMap()
        if (dateValues.isEmpty()) mapOf<LocalDate, Number>()
        else DateRange(dateValues.keys.first()..(date ?: dateValues.keys.last())).map {
            val values = dateValues[it] ?: listOf()
            val integers = values.all { it is Int }
            val sum: Number = if (integers) values.sumBy { it?.toInt() ?: 0 } else values.sumByDouble { it?.toDouble() ?: 0.0 }
            it to sum
        }.toMap()
    }),

    /** Fills latest value, ensuring nonzero entries. */
    FILL_WITH_LATEST_VALUE({ pairs, date ->
        val dateValues = pairs.map { it.first to it.second }.toMap().toSortedMap()
        if (dateValues.isEmpty()) mapOf<LocalDate, Number>()
        else DateRange(dateValues.keys.first()..(date ?: dateValues.keys.last())).map {
            it to if (it in dateValues.keys) dateValues[it]!!
            else dateValues.headMap(it).values.last()!!
        }.toMap()
    });

    operator fun invoke(entries: List<Pair<LocalDate, Number>>, date: LocalDate?) = aggregator(entries, date)
}