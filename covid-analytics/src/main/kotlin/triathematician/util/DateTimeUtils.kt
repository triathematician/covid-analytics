package triathematician.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

operator fun LocalDate.rangeTo(other: LocalDate) = DateRange(this, other)

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

class DateIterator(startDate: LocalDate, val endDateInclusive: LocalDate): Iterator<LocalDate> {
    private var currentDate = startDate
    override fun hasNext() = currentDate <= endDateInclusive
    override fun next(): LocalDate {
        val next = currentDate
        currentDate += 1
        return next
    }
}

data class DateRange(override var start: LocalDate, override var endInclusive: LocalDate): Iterable<LocalDate>, ClosedRange<LocalDate> {
    override fun iterator() = DateIterator(start, endInclusive)

    /** Number of days in range. */
    val size
        get() = endInclusive - start + 1

    /** Adds given number of days to start and end of range. */
    fun shift(startDelta: Int, endDelta: Int) = DateRange(start + startDelta, endInclusive + endDelta)
    /** Takes the last n days from the range. */
    fun tail(n: Int) = when {
        n > size -> shift(size.toInt() - n, 0)
        else -> this
    }
}

operator fun LocalDate.minus(other: LocalDate) = ChronoUnit.DAYS.between(other, this)
operator fun LocalDate.plus(days: Int) = plusDays(days.toLong())
operator fun LocalDate.minus(days: Int) = minusDays(days.toLong())
