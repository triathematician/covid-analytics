package triathematician.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** Formats date as month and day only. */
val LocalDate.monthDay
    get() = this.format(DateTimeFormatter.ofPattern("M/d"))

/** Parses string to local date using one of given formats. */
fun String.toLocalDate(vararg formats: DateTimeFormatter): LocalDate {
    formats.forEach {
        try {
            return LocalDate.from(it.parse(this))
        } catch (x: DateTimeParseException) {
            // skip it
        }
    }
    return LocalDate.parse(this)
}

/** Get number of days between two dates. */
operator fun LocalDate.minus(other: LocalDate) = ChronoUnit.DAYS.between(other, this)
/** Add number of days to date. */
operator fun LocalDate.plus(days: Number) = plusDays(days.toLong())
/** Subtract number of days from date. */
operator fun LocalDate.minus(days: Number) = minusDays(days.toLong())

/** Produces a date range. */
operator fun LocalDate.rangeTo(other: LocalDate) = DateRange(this, other)

/** Provides a range of dates, with ability to iterate. */
data class DateRange(override var start: LocalDate, override var endInclusive: LocalDate): Iterable<LocalDate>, ClosedRange<LocalDate> {
    override fun iterator() = DateIterator(start, endInclusive)

    /** Number of days in range. */
    val size
        get() = endInclusive - start + 1

    /** Adds given number of days to start and end of range. */
    fun shift(startDelta: Int, endDelta: Int) = DateRange(start + startDelta, endInclusive + endDelta)
    /** Takes the last n days from the range. */
    fun tail(n: Int) = when {
        size > n -> shift(size.toInt() - n, 0)
        else -> this
    }

    /** Intersects with another domain. */
    fun intersect(other: DateRange) = DateRange(maxOf(start, other.start), minOf(endInclusive, other.endInclusive)).let {
        if (size < 0) null else it
    }
}

/** Iterates between two dates. */
class DateIterator(startDate: LocalDate, val endDateInclusive: LocalDate): Iterator<LocalDate> {
    private var currentDate = startDate
    override fun hasNext() = currentDate <= endDateInclusive
    override fun next(): LocalDate {
        val next = currentDate
        currentDate += 1
        return next
    }
}