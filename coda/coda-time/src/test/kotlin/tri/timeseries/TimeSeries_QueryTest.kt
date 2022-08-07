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
import tri.area.AreaInfo
import tri.area.AreaLookup
import tri.area.UNKNOWN
import tri.area.USA
import tri.util.DateRange
import kotlin.time.ExperimentalTime
import kotlin.test.assertEquals

@ExperimentalTime
class TimeSeries_QueryTest {

    private val FIRST_DATE = date(2, 1)
    private val LAST_DATE = date(2, 2)
    private val TEST_SERIES = testSeries(1, 2)
    private val TEST_SERIES_NAN = testSeries(defaultValue = Double.NaN, 1, 2)

    @Test
    fun testValuesAsMap() {
        assertEquals(mapOf(FIRST_DATE to 1.0, LAST_DATE to 2.0), TEST_SERIES.valuesAsMap)
    }

    @Test
    fun testDate() {
        assertEquals(FIRST_DATE, TEST_SERIES.date(0))
        assertEquals(LAST_DATE, TEST_SERIES.date(1))
        assertEquals(date(2, 3), TEST_SERIES.date(2))
    }

    @Test
    fun testGet() {
        assertEquals(1.0, TEST_SERIES[FIRST_DATE])
        assertEquals(2.0, TEST_SERIES[LAST_DATE])
        assertEquals(0.0, TEST_SERIES[date(1, 1)])
        assertEquals(Double.NaN, TEST_SERIES_NAN[date(1, 1)])
    }

    @Test
    fun testGetOrNull() {
        assertEquals(1.0, TEST_SERIES.getOrNull(FIRST_DATE))
        assertEquals(2.0, TEST_SERIES.getOrNull(LAST_DATE))
        assertEquals(null, TEST_SERIES.getOrNull(date(1, 31)))
        assertEquals(null, TEST_SERIES.getOrNull(date(2, 5)))
        assertEquals(null, TEST_SERIES_NAN.getOrNull(date(2, 5)))
    }

    @Test
    fun testValues() {
        assertEquals(listOf(0.0, 1.0, 2.0, 0.0),
                TEST_SERIES.values(DateRange(date(1, 31)..date(2, 3))))
        assertEquals(listOf(Double.NaN, 1.0, 2.0, Double.NaN),
                TEST_SERIES_NAN.values(DateRange(date(1, 31)..date(2, 3))))
    }

    @Test
    fun testValuesByDaysFromEnd() {
        assertEquals(0.0, TEST_SERIES.valueByDaysFromEnd(-1))
        assertEquals(2.0, TEST_SERIES.valueByDaysFromEnd(0))
        assertEquals(1.0, TEST_SERIES.valueByDaysFromEnd(1))
        assertEquals(0.0, TEST_SERIES.valueByDaysFromEnd(2))

        assertEquals(Double.NaN, TEST_SERIES_NAN.valueByDaysFromEnd(-1))
        assertEquals(2.0, TEST_SERIES_NAN.valueByDaysFromEnd(0))
        assertEquals(1.0, TEST_SERIES_NAN.valueByDaysFromEnd(1))
        assertEquals(Double.NaN, TEST_SERIES_NAN.valueByDaysFromEnd(2))
    }

    @Test
    fun testLast() {
        assertEquals(listOf(2.0), TEST_SERIES.last(0..0))
        assertEquals(listOf(1.0), TEST_SERIES.last(1..1))
        assertEquals(listOf(1.0, 2.0), TEST_SERIES.last(0..1))
        assertEquals(listOf(1.0, 2.0), TEST_SERIES.last(-1..2))
        assertEquals(listOf(), TEST_SERIES.last(-2..-1))
    }

}
