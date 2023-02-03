/*-
 * #%L
 * coda-time-0.4.0-SNAPSHOT
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

import java.time.LocalDate
import kotlin.test.assertEquals

internal fun date(month: Int, dayOfMonth: Int) = LocalDate.of(2000, month, dayOfMonth)

internal fun testSeries(area: String, vararg values: Number) = testSeries(area, 0.0, values.toList())
internal fun testSeries(defaultValue: Double, vararg values: Number) = testSeries("a", defaultValue = defaultValue, values.toList())
internal fun testSeries(vararg values: Number) = testSeries("a", 0.0, values.toList())
internal fun testSeries(values: List<Number>) = testSeries("a", 0.0, values)

internal fun testSeries(area: String, defaultValue: Double, values: List<Number>) =
        TimeSeries("s", area, "m", "q", true, defaultValue, date(2, 1), values.map { it.toDouble() })

internal fun TimeSeries.assertTimeSeries(date: LocalDate, vararg expectedValues: Number) {
    assertEquals(expectedValues.map { it.toDouble() }, values)
    assertEquals(date, start)
}
