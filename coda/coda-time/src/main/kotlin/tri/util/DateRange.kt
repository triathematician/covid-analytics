package tri.timeseries

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Provides a range of dates, with ability to iterate. */
data class DateRange(override var start: LocalDate, override var endInclusive: LocalDate): Iterable<LocalDate>, ClosedRange<LocalDate> {

    constructor(range: ClosedRange<LocalDate>): this(range.start, range.endInclusive)
    constructor(dates: Collection<LocalDate>): this(dates.minOrNull()!!, dates.maxOrNull()!!)

    override fun iterator() = object : Iterator<LocalDate> {
        private var currentDate = start
        override fun hasNext() = currentDate <= endInclusive
        override fun next(): LocalDate {
            val next = currentDate
            currentDate = currentDate.plusDays(1L)
            return next
        }
    }

    /** Number of days in range. */
    @get:JsonIgnore
    val size
        get() = ChronoUnit.DAYS.between(start, endInclusive) + 1

    @JsonIgnore
    override fun isEmpty() = super.isEmpty()

    //region DERIVATIONS

    /** Adds given number of days to start and end of range. */
    fun shift(startDelta: Int, endDelta: Int) = DateRange(start.plusDays(startDelta.toLong()), endInclusive.plusDays(endDelta.toLong()))

    /** Takes the last n days from the range. */
    fun tail(n: Int) = when {
        size > n -> shift(size.toInt() - n, 0)
        else -> this
    }

    /** Intersects with another domain. */
    fun intersect(other: DateRange) = DateRange(maxOf(start, other.start), minOf(endInclusive, other.endInclusive)).let {
        if (size < 0) null else it
    }

    //endregion
}

/** Produces a date range. */
operator fun LocalDate.rangeTo(other: LocalDate) = DateRange(this, other)

/** Get range of dates in month. */
val YearMonth.dateRange
    get() = atDay(1)..atEndOfMonth()