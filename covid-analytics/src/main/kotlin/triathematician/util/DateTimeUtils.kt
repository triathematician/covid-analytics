package triathematician.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

operator fun LocalDate.rangeTo(other: LocalDate) = DateRange(this, other)

fun String.toLocalDate(vararg formats: DateTimeFormatter): LocalDate {
    formats.forEach {
        try {
            return LocalDate.from(it.parse(this))
        } catch (x: DateTimeParseException) {
            // ksip it
        }
    }
    return LocalDate.parse(this)
}

class DateIterator(val startDate: LocalDate, val endDateInclusive: LocalDate): Iterator<LocalDate> {
    private var currentDate = startDate
    override fun hasNext() = currentDate <= endDateInclusive
    override fun next(): LocalDate {
        val next = currentDate
        currentDate = currentDate.plusDays(1)
        return next
    }
}

class DateRange(override var start: LocalDate, override var endInclusive: LocalDate): Iterable<LocalDate>, ClosedRange<LocalDate> {
    override fun iterator() = DateIterator(start, endInclusive)
}
