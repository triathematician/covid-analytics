/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
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

import org.junit.Test
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals
import kotlin.test.fail

@ExperimentalTime
class TimeSeriesTest {

    @Test
    fun testCopyWithDataSince() {
        val t = testSeries(listOf(1, 2, 3, 4))

        t.copyWithDataSince(date(1, 30))
                .assertTimeSeries(date(2, 1), 1, 2, 3, 4)

        t.copyWithDataSince(date(2, 2))
                .assertTimeSeries(date(2, 2), 2, 3, 4)
        t.copyWithDataSince(date(2, 10))
                .assertTimeSeries(date(2, 10))
    }

    @Test
    fun testCopyExtendedThrough() {
        val t = testSeries(listOf(1, 2, 3, 4))

        t.copyExtendedThrough(date(1, 30), TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(date(2, 1), 1, 2, 3, 4)

        t.copyExtendedThrough(date(2, 2), TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(date(2, 1), 1, 2, 3, 4)

        t.copyExtendedThrough(date(2, 6), TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(date(2, 1), 1, 2, 3, 4, 0, 0)
        t.copyExtendedThrough(date(2, 6), TimeSeriesFillStrategy.FILL_FORWARD)
                .assertTimeSeries(date(2, 1), 1, 2, 3, 4, 4, 4)
    }

    @Test
    fun testPercentChanges() {
        val values = listOf(1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2)
        val t = testSeries(values)

        assertEquals(listOf(0.0, 0.0, 1.0, 0.0, 0.0, 0.5, 0.0, 0.0, -1.0, Double.NaN, Double.NaN, Double.POSITIVE_INFINITY, 0.0, 0.0, 1.0, 0.0, 0.0),
            t.percentChanges(1, 1).values)
        assertEquals(listOf(0.5, 1.0, 0.3333333333333333, 0.25, 0.5, 0.2, -0.5, -1.0, -1.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0, 0.5, 1.0, 0.3333333333333333),
            t.percentChanges(2, 2).values)
        assertEquals(listOf(-0.3333333333333333, -0.5714285714285714, -0.6874999999999999, -0.5333333333333333, -0.30769230769230765),
            t.percentChanges(7, 7).values)

        assertEquals(date(2, 14), t.percentChanges(7, 7).start)
    }

    @Test
    fun testDropFirst() {
        val t = testSeries(listOf(1, 2, 3, 4))

        try {
        t.dropFirst(-2).assertTimeSeries(date(2, 3), 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }
        t.dropFirst(0).assertTimeSeries(date(2, 1), 1, 2, 3, 4)
        t.dropFirst(2).assertTimeSeries(date(2, 3), 3, 4)
    }

    @Test
    fun testDropLast() {
        val t = testSeries(listOf(1, 2, 3, 4))

        try {
            t.dropLast(-2).assertTimeSeries(date(2, 1), 1, 2, 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }
        t.dropLast(0).assertTimeSeries(date(2, 1), 1, 2, 3, 4)
        t.dropLast(2).assertTimeSeries(date(2, 1), 1, 2)
    }

    @Test
    fun testTrim() {
        val t = testSeries(listOf(1, 2, 3, 4))

        try {
            t.trim(-1, 1).assertTimeSeries(date(2, 1), 1, 2, 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }
        try {
            t.trim(1, -1).assertTimeSeries(date(2, 1), 1, 2, 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }

        t.trim(1, 1).assertTimeSeries(date(2, 2), 2, 3)
        t.trim(0, 2).assertTimeSeries(date(2, 1), 1, 2)
        t.trim(2, 0).assertTimeSeries(date(2, 3), 3, 4)
    }

    @Test
    fun testAdjustDates() {
        val t = testSeries(listOf(1, 2, 3, 4))

        t.adjustDates(date(1, 31), date(2, 5))
                .assertTimeSeries(date(2, 1), 1, 2, 3, 4)

        t.adjustDates(date(1, 31), date(2, 5), TimeSeriesFillStrategy.FILL_WITH_ZEROS, TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(date(1, 31), 0, 1, 2, 3, 4, 0)
        t.adjustDates(date(1, 31), date(2, 5), TimeSeriesFillStrategy.FILL_BACKWARD, TimeSeriesFillStrategy.FILL_FORWARD)
                .assertTimeSeries(date(1, 31), 1, 1, 2, 3, 4, 4)

        t.adjustDates(date(2, 2), date(2, 3))
                .assertTimeSeries(date(2, 2), 2, 3)
        t.adjustDates(date(2, 2), date(2, 10), fillForward = TimeSeriesFillStrategy.FILL_BACKWARD)
                .assertTimeSeries(date(2, 2), 2, 3, 4)
        t.adjustDates(date(2, 2), date(2, 10), fillForward = TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(date(2, 2), 2, 3, 4, 0, 0, 0, 0, 0, 0)
    }

    private fun date(month: Int, dayOfMonth: Int) = LocalDate.of(2000, month, dayOfMonth)

    private fun testSeries(values: List<Int>) =
            TimeSeries("", "", "", "", 0, date(2, 1), values)

    private fun TimeSeries.assertTimeSeries(date: LocalDate, vararg expectedValues: Number) {
        assertEquals(expectedValues.map { it.toDouble() }, values)
        assertEquals(date, start)
    }

}
