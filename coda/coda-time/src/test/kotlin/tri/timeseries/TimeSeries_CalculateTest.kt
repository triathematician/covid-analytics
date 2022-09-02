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
import tri.util.DateRange
import tri.util.yearMonth
import tri.util.minus
import tri.util.plus
import kotlin.time.ExperimentalTime
import kotlin.test.assertEquals
import kotlin.test.fail

class TimeSeries_CalculateTest {

    private val FIRST_DATE = date(2, 1)
    private val SECOND_DATE = date(2, 2)
    private val THIRD_DATE = date(2, 3)
    private val LAST_DATE = date(2, 4)
    private val TEST_SERIES = testSeries(1, 2, 3, 4)
    private val TEST_SERIES_NAN = testSeries(defaultValue = Double.NaN, 1, 2, 3, 4)
    private val TEST_SERIES_NAN2 = testSeries(defaultValue = Double.NaN, 1, Double.NaN, Double.NaN, 2)
    private val EMPTY_SERIES = testSeries()

    @Test
    fun testSum_DateRange() {
        assertEquals(10.0, TEST_SERIES.sum(TEST_SERIES.domain))
        assertEquals(5.0, TEST_SERIES.sum(DateRange(SECOND_DATE..THIRD_DATE)))
        assertEquals(9.0, TEST_SERIES.sum(DateRange(SECOND_DATE..date(2, 8))))
        assertEquals(0.0, TEST_SERIES.sum(DateRange(date(2, 8)..date(2, 8))))
    }

    @Test
    fun testAverage_DateRange() {
        assertEquals(2.5, TEST_SERIES.average(TEST_SERIES.domain))
        assertEquals(2.5, TEST_SERIES.average(DateRange(SECOND_DATE..THIRD_DATE)))
        assertEquals(9.0/7.0, TEST_SERIES.average(DateRange(SECOND_DATE..date(2, 8))))
        assertEquals(0.0, TEST_SERIES.average(DateRange(date(2, 8)..date(2, 8))))
    }

    @Test
    fun testSum_YearMonth() {
        assertEquals(10.0, TEST_SERIES.sum(FIRST_DATE.yearMonth))
        assertEquals(0.0, TEST_SERIES.sum(date(1, 31).yearMonth))
    }

    @Test
    fun testAverage_YearMonth() {
        assertEquals(10.0/29.0, TEST_SERIES.average(FIRST_DATE.yearMonth))
        assertEquals(0.0, TEST_SERIES.average(date(1, 31).yearMonth))
    }

    @Test
    fun testTransform() {
        TEST_SERIES.transform { 2 * it + 1 }
                .assertTimeSeries(FIRST_DATE, 3, 5, 7, 9)
    }

    @Test
    fun testPlus() {
        (TEST_SERIES + 1)
                .assertTimeSeries(FIRST_DATE, 2, 3, 4, 5)

        TEST_SERIES.plus(TEST_SERIES)
                .assertTimeSeries(FIRST_DATE, 2, 4, 6, 8)
        TEST_SERIES.plus(TEST_SERIES.copy(start = SECOND_DATE))
                .assertTimeSeries(FIRST_DATE, 1, 3, 5, 7, 4)
        TEST_SERIES.plus(TEST_SERIES.copy(start = LAST_DATE + 2))
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4, 0, 1, 2, 3, 4)
    }

    @Test
    fun testMinus() {
        (TEST_SERIES - 1)
            .assertTimeSeries(FIRST_DATE, 0, 1, 2, 3)

        TEST_SERIES.minus(TEST_SERIES)
                .assertTimeSeries(FIRST_DATE, 0, 0, 0, 0)
        TEST_SERIES.minus(TEST_SERIES.copy(start = SECOND_DATE))
                .assertTimeSeries(FIRST_DATE, 1, 1, 1, 1, -4)
        TEST_SERIES.minus(TEST_SERIES.copy(start = LAST_DATE + 2))
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4, 0, -1, -2, -3, -4)
    }

    @Test
    fun testTimes() {
        (TEST_SERIES * 2)
                .assertTimeSeries(FIRST_DATE, 2, 4, 6, 8)

        TEST_SERIES.times(TEST_SERIES)
                .assertTimeSeries(FIRST_DATE, 1, 4, 9, 16)
        TEST_SERIES.times(TEST_SERIES.copy(start = SECOND_DATE))
                .assertTimeSeries(FIRST_DATE, 0, 2, 6, 12, 0)
        TEST_SERIES.times(TEST_SERIES.copy(start = LAST_DATE + 2))
                .assertTimeSeries(FIRST_DATE, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }

    @Test
    fun testDiv() {
        (TEST_SERIES / 2)
                .assertTimeSeries(FIRST_DATE, 0.5, 1.0, 1.5, 2.0)

        TEST_SERIES.div(TEST_SERIES)
                .assertTimeSeries(FIRST_DATE, 1, 1, 1, 1)
        TEST_SERIES.div(TEST_SERIES.copy(start = SECOND_DATE))
                .assertTimeSeries(FIRST_DATE, Double.POSITIVE_INFINITY, 2.0, 1.5, 4.0/3, 0)
        TEST_SERIES.div(TEST_SERIES.copy(start = LAST_DATE + 2))
                .assertTimeSeries(FIRST_DATE, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN, 0, 0, 0, 0)
    }

    @Test
    fun testMovingAverage() {
        TEST_SERIES.movingAverage(-1)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
        TEST_SERIES.movingAverage(0)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
        TEST_SERIES.movingAverage(1)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)

        TEST_SERIES.movingAverage(2)
                .assertTimeSeries(FIRST_DATE, 1, 1.5, 2.5, 3.5)
        TEST_SERIES.movingAverage(3)
                .assertTimeSeries(FIRST_DATE, 1, 1.5, 2, 3)

        TEST_SERIES.movingAverage(2, includePartialList = false)
                .assertTimeSeries(FIRST_DATE + 1, 1.5, 2.5, 3.5)
        TEST_SERIES.movingAverage(3, includePartialList = false)
                .assertTimeSeries(FIRST_DATE + 2, 2, 3)

        testSeries(1, 0, 0, 3).movingAverage(2, nonZero = true, includePartialList = false)
                .assertTimeSeries(FIRST_DATE + 1, 1, Double.NaN, 3)
        testSeries(1, 0, 0, 3, 2, 1).movingAverage(3, nonZero = true, includePartialList = false)
                .assertTimeSeries(FIRST_DATE + 2, 1, 3, 2.5, 2)
    }

    @Test
    fun testMovingSum() {
        TEST_SERIES.movingSum(-1)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
        TEST_SERIES.movingSum(0)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)
        TEST_SERIES.movingSum(1)
                .assertTimeSeries(FIRST_DATE, 1, 2, 3, 4)

        TEST_SERIES.movingSum(2)
                .assertTimeSeries(FIRST_DATE, 1, 3, 5, 7)
        TEST_SERIES.movingSum(3)
                .assertTimeSeries(FIRST_DATE, 1, 3, 6, 9)

        TEST_SERIES.movingSum(2, includePartialList = false)
                .assertTimeSeries(FIRST_DATE + 1, 3, 5, 7)
        TEST_SERIES.movingSum(3, includePartialList = false)
                .assertTimeSeries(FIRST_DATE + 2, 6, 9)
    }

    @Test
    fun testDeltas() {
        EMPTY_SERIES.deltas()
                .assertTimeSeries(FIRST_DATE)

        TEST_SERIES.deltas()
                .assertTimeSeries(FIRST_DATE, 1, 1, 1, 1)
        TEST_SERIES.deltas(2)
                .assertTimeSeries(FIRST_DATE, 1, 2, 2, 2)

        TEST_SERIES_NAN.deltas()
                .assertTimeSeries(FIRST_DATE, Double.NaN, 1, 1, 1)
        TEST_SERIES_NAN2.deltas()
                .assertTimeSeries(FIRST_DATE, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
    }

    @Test
    fun testCumulative() {
        TEST_SERIES.cumulative()
                .assertTimeSeries(FIRST_DATE, 1, 3, 6, 10)
    }

    @Test
    fun testCumulativeSince() {
        TEST_SERIES.cumulativeSince(FIRST_DATE + 1)
                .assertTimeSeries(FIRST_DATE + 1, 2, 5, 9)
        TEST_SERIES.cumulativeSince(FIRST_DATE)
                .assertTimeSeries(FIRST_DATE, 1, 3, 6, 10)
        TEST_SERIES.cumulativeSince(FIRST_DATE - 1)
                .assertTimeSeries(FIRST_DATE - 1, 0, 1, 3, 6, 10)
    }

    @Test
    fun testAbsoluteChanges() {
        EMPTY_SERIES.absoluteChanges(1)
                .assertTimeSeries(FIRST_DATE)

        TEST_SERIES.absoluteChanges(0)
                .assertTimeSeries(FIRST_DATE, 0, 0, 0, 0)
        TEST_SERIES.absoluteChanges(1)
                .assertTimeSeries(FIRST_DATE + 1, 1, 1, 1)
        TEST_SERIES.absoluteChanges(2)
                .assertTimeSeries(FIRST_DATE + 2, 2, 2)

        TEST_SERIES_NAN.absoluteChanges(1)
                .assertTimeSeries(FIRST_DATE + 1, 1, 1, 1)
        TEST_SERIES_NAN2.absoluteChanges(1)
                .assertTimeSeries(FIRST_DATE + 1, Double.NaN, Double.NaN, Double.NaN)
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
    fun testPeak() {
        assertEquals(null, EMPTY_SERIES.peak())

        assertEquals(LAST_DATE to 4.0, TEST_SERIES.peak())
        assertEquals(LAST_DATE to 4.0, TEST_SERIES.peak(LAST_DATE))
        assertEquals(FIRST_DATE to 4.0, testSeries(4, 3, 2, 1).peak())
        assertEquals(THIRD_DATE to 2.0, testSeries(4, 3, 2, 1).peak(THIRD_DATE))

        assertEquals(LAST_DATE to Double.POSITIVE_INFINITY, testSeries(1, 2, 3, Double.POSITIVE_INFINITY).peak())
        assertEquals(LAST_DATE.minusDays(1L) to 3.0, testSeries(1, 2, 3, Double.NEGATIVE_INFINITY).peak())
        assertEquals(LAST_DATE.minusDays(1L) to 3.0, testSeries(1, 2, 3, Double.NaN).peak())
    }

    @Test
    fun testDaysSinceHalfCurrentValue() {
        assertEquals(2, TEST_SERIES.daysSinceHalfCurrentValue())
        assertEquals(null, testSeries(0).daysSinceHalfCurrentValue())
        assertEquals(null, testSeries(3, 2, 1).daysSinceHalfCurrentValue())
    }

    @Test
    fun testGrowthRates() {
        testSeries(1, 2, 3, 4, 0, 1).growthRates()
                .assertTimeSeries(FIRST_DATE + 1, 2, 1.5, 4.0/3, 0.0, Double.POSITIVE_INFINITY)
    }

    @Test
    fun testSymmetricGrowth() {
        testSeries(1, 2, 3, 4, 0, 1, -1).symmetricGrowth()
                .assertTimeSeries(FIRST_DATE + 1, 2.0/3, 0.4, 2.0/7, -2.0, 2.0, Double.NEGATIVE_INFINITY)
    }

    @Test
    fun testDoublingTimes() {
        testSeries(1, 2, 3, 4, 0, 1, -1).doublingTimes()
                .assertTimeSeries(FIRST_DATE + 1, 1.0, 1.7095112913514547, 2.4094208396532095, -0.0, 0.0, Double.NaN)
    }

}
