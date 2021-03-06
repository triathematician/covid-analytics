/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.util

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities

/** Get month/year of date. */
val LocalDate.yearMonth
    get() = YearMonth.of(year, month)
/** Get number of days in a month. */
val LocalDate.daysInMonth
    get() = yearMonth.lengthOfMonth()
/** Test if date is same month as another date. */
fun LocalDate.sameMonthAs(other: LocalDate) = yearMonth == other.yearMonth

//region FORMATTING

/** Formats date like "15-Sep". */
val LocalDate.dayDashMonth
    get() = this.format(DateTimeFormatter.ofPattern("d-MMM"))
/** Formats date as month and day only. */
val LocalDate.monthDay
    get() = this.format(DateTimeFormatter.ofPattern("M/d"))
/** Formats date as month/day/year. */
val LocalDate.monthDayYear
    get() = DateTimeFormatter.ofPattern("M/d/yyyy").format(this)
/** Formats date as readable month-day. */
val LocalDate.monthDayReadable
    get() = this.format(DateTimeFormatter.ofPattern("MMMM d"))
/** Formats date as year-month-day. */
val LocalDate.yyyyMMdd
    get() = DateTimeFormatter.ofPattern("yyyyMMdd").format(this)
val LocalDate.yyyy_MM_dd
    get() = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(this)

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

//endregion

//region DATE OPERATORS

/** Add number of months to date. */
operator fun YearMonth.plus(days: Number) = this.plusMonths(days.toLong())
/** Subtract number of months to date. */
operator fun YearMonth.minus(days: Number) = this.minusMonths(days.toLong())

/** Get number of days between two dates. */
operator fun LocalDate.minus(other: LocalDate) = ChronoUnit.DAYS.between(other, this)
/** Add number of days to date. */
operator fun LocalDate.plus(days: Number) = this.plusDays(days.toLong())
/** Subtract number of days from date. */
operator fun LocalDate.minus(days: Number) = minusDays(days.toLong())

//endregion

//region DATE RANGES

/** Get range of dates in month. */
val YearMonth.dateRange
    get() = atDay(1)..atEndOfMonth()

/** Produces a date range. */
operator fun LocalDate.rangeTo(other: LocalDate) = DateRange(this, other)

private fun Iterable<LocalDate>.enclosingRange(): ClosedRange<LocalDate> {
    val set = toSortedSet()
    return set.first()..set.last()
}

/** Provides a range of dates, with ability to iterate. */
data class DateRange(override var start: LocalDate, override var endInclusive: LocalDate): Iterable<LocalDate>, ClosedRange<LocalDate> {

    constructor(range: ClosedRange<LocalDate>): this(range.start, range.endInclusive)
    constructor(dates: Collection<LocalDate>): this(dates.enclosingRange())

    override fun iterator() = DateIterator(start, endInclusive)

    /** Number of days in range. */
    @get:JsonIgnore
    val size
        get() = endInclusive - start + 1

    @JsonIgnore
    override fun isEmpty() = super.isEmpty()

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

//endregion
