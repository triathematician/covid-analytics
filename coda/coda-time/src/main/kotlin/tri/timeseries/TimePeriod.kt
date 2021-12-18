/*-
 * #%L
 * coda-data-0.1.4-SNAPSHOT
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
package tri.timeseries

import tri.util.yearMonth
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.Temporal

/** Time periods across which [TimeSeries] can be specified or aggregated. */
enum class TimePeriod(val type: Class<out Temporal>) {
    YEAR(Year::class.java),
    QUARTER(YearMonth::class.java),
    MONTH(YearMonth::class.java),
    MONDAY_WEEK(LocalDate::class.java),
    SUNDAY_WEEK(LocalDate::class.java),
    DAILY(LocalDate::class.java);

    fun toWeek(date: LocalDate) = date.minusDays(date.dayOfWeek.value % 7L)
    fun toMondayWeek(date: LocalDate) = date.minusDays(date.dayOfWeek.value - 1L)
    fun toMonth(date: LocalDate) = date.yearMonth
    fun toQuarter(date: LocalDate) = MONTH.toQuarter(toMonth(date))
    fun toYear(date: LocalDate) = Year.from(date)

    fun toQuarter(month: YearMonth) = month.minusMonths((month.monthValue - 1) % 3L)
    fun toYear(month: YearMonth) = Year.from(month)
}