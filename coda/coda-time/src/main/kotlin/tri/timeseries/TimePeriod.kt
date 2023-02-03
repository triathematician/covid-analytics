/*-
 * #%L
 * coda-data-0.1.4-SNAPSHOT
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
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

/** Common time periods across which [TimeSeries] data can be aggregated. */
enum class TimePeriod(val daysPerValue: Int? = null) {
    CUMULATIVE,
    LATEST(1),
    DAILY(1),
    WEEKLY_TOTAL(7),
    WEEKLY_AVERAGE(1),
    BIWEEKLY_TOTAL(14),
    BIWEEKLY_AVERAGE(1),
    MONTHLY_TOTAL,
    MONTHLY_AVERAGE(1),
    YEARLY_TOTAL,
    YEARLY_AVERAGE(1),

    WEEKLY_TOTAL_DIFFERENCE,
    WEEKLY_AVERAGE_DIFFERENCE,
    WEEKLY_PERCENT_CHANGE,
    WEEKLY_CUMULATIVE_PERCENT_CHANGE
    ;
}
