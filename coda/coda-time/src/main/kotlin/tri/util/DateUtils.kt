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

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** Today's date (as of runtime). */
val TODAY = LocalDate.now()
/** Yesterday's date (as of runtime). */
val YESTERDAY = TODAY - 1

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

//endregion
