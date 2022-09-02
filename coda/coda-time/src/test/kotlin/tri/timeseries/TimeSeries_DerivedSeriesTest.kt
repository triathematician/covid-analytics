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
import kotlin.time.ExperimentalTime
import java.lang.IllegalArgumentException
import kotlin.test.fail

class TimeSeries_DerivedSeriesTest {

    private val BEFORE_DATE = date(1, 31)
    private val FIRST_DATE = date(2, 1)
    private val SECOND_DATE = date(2, 2)
    private val THIRD_DATE = date(2, 3)
    private val LAST_DATE = date(2, 4)
    private val AFTER_DATE = date(2, 5)

    private val TEST_SERIES = testSeries(1, 2, 3, 4)

    @Test
    fun testCopyWithDataSince() {
        TEST_SERIES.copyWithDataSince(date(1, 30))
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)

        TEST_SERIES.copyWithDataSince(SECOND_DATE)
                .assertTimeSeries(SECOND_DATE, 2, 3, 4)
        TEST_SERIES.copyWithDataSince(date(2, 10))
                .assertTimeSeries(date(2, 10))
    }
    
    @Test
    fun testCopyAdjustingStartDay() {
        TEST_SERIES.copyAdjustingStartDay()
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
        TEST_SERIES.copyAdjustingStartDay(values = listOf(2.0, 3.0))
                .assertTimeSeries(THIRD_DATE, 2, 3)
        TEST_SERIES.copyAdjustingStartDay(values = listOf(1.0, 2.0, 3.0, 4.0, 5.0))
                .assertTimeSeries(BEFORE_DATE, 1, 2, 3, 4, 5)
    }

    @Test
    fun testCopyExtendedThrough() {
        TEST_SERIES.copyExtendedThrough(date(1, 30), TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)

        TEST_SERIES.copyExtendedThrough(SECOND_DATE, TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)

        TEST_SERIES.copyExtendedThrough(date(2, 6), TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4, 0, 0)
        TEST_SERIES.copyExtendedThrough(date(2, 6), TimeSeriesFillStrategy.FILL_FORWARD)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4, 4, 4)
    }

    @Test
    fun testDropFirst() {
        try {
            TEST_SERIES.dropFirst(-2).assertTimeSeries(THIRD_DATE, 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }
        TEST_SERIES.dropFirst(0).assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
        TEST_SERIES.dropFirst(2).assertTimeSeries(THIRD_DATE, 3, 4)
    }

    @Test
    fun testDropLast() {
        try {
            TEST_SERIES.dropLast(-2).assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }
        TEST_SERIES.dropLast(0).assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
        TEST_SERIES.dropLast(2).assertTimeSeries(FIRST_DATE, 1, 2)
    }

    @Test
    fun testTrim() {
        try {
            TEST_SERIES.trim(-1, 1).assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }
        try {
            TEST_SERIES.trim(1, -1).assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
            fail()
        } catch (x: IllegalArgumentException) {
            // expected
        }

        TEST_SERIES.trim(1, 1).assertTimeSeries(SECOND_DATE, 2, 3)
        TEST_SERIES.trim(0, 2).assertTimeSeries(FIRST_DATE, 1, 2)
        TEST_SERIES.trim(2, 0).assertTimeSeries(THIRD_DATE, 3, 4)
    }

    @Test
    fun testAdjustDates() {
        TEST_SERIES.adjustDates(BEFORE_DATE, AFTER_DATE)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)

        TEST_SERIES.adjustDates(BEFORE_DATE, AFTER_DATE, TimeSeriesFillStrategy.FILL_WITH_ZEROS, TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(BEFORE_DATE, 0, 1, 2, 3, 4, 0)
        TEST_SERIES.adjustDates(BEFORE_DATE, AFTER_DATE, TimeSeriesFillStrategy.FILL_BACKWARD, TimeSeriesFillStrategy.FILL_FORWARD)
                .assertTimeSeries(BEFORE_DATE, 1, 1, 2, 3, 4, 4)

        TEST_SERIES.adjustDates(SECOND_DATE, THIRD_DATE)
                .assertTimeSeries(SECOND_DATE, 2, 3)
        TEST_SERIES.adjustDates(SECOND_DATE, date(2, 10), fillForward = TimeSeriesFillStrategy.FILL_BACKWARD)
                .assertTimeSeries(SECOND_DATE, 2, 3, 4)
        TEST_SERIES.adjustDates(SECOND_DATE, date(2, 10), fillForward = TimeSeriesFillStrategy.FILL_WITH_ZEROS)
                .assertTimeSeries(SECOND_DATE, 2, 3, 4, 0, 0, 0, 0, 0, 0)
    }

}
