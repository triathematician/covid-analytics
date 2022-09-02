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
import kotlin.time.ExperimentalTime
import kotlin.test.assertEquals

class TimeSeries_ComputedAttributeTest {

    private val FIRST_DATE = date(2, 1)
    private val LAST_DATE = date(2, 2)
    private val TEST_SERIES = testSeries(1, 2)
    private val TEST_SERIES_EMPTY = testSeries()

    @Test
    fun testUniqueMetricKey() {
        assertEquals("s::a::m::q", TEST_SERIES.uniqueMetricKey)
    }

    @Test
    fun testMetricInfo() {
        assertEquals(MetricInfo("m", "q"), TEST_SERIES.metricInfo)
    }

    @Test
    fun testDomain() {
        assertEquals(DateRange(FIRST_DATE..LAST_DATE), TEST_SERIES.domain)
        assertEquals(listOf(), TEST_SERIES_EMPTY.domain.toList())
    }

    @Test
    fun testEnd() {
        assertEquals(LAST_DATE, TEST_SERIES.end)
        assertEquals(FIRST_DATE.minusDays(1L), TEST_SERIES_EMPTY.end)
    }

    @Test
    fun testFirstPositiveDate() {
        assertEquals(FIRST_DATE, TEST_SERIES.firstPositiveDate)
        assertEquals(LAST_DATE, testSeries(0, 2).firstPositiveDate)
        assertEquals(null, testSeries(0, 0).firstPositiveDate)
    }

    @Test
    fun testSize() {
        assertEquals(2, TEST_SERIES.size)
        assertEquals(2, testSeries(0, 0).size)
    }

    @Test
    fun testValuesAsMap() {
        assertEquals(mapOf(FIRST_DATE to 1.0, LAST_DATE to 2.0), TEST_SERIES.valuesAsMap)
    }

    @Test
    fun testLastValue() {
        assertEquals(2.0, TEST_SERIES.lastValue)
        assertEquals(0.0, TEST_SERIES_EMPTY.lastValue)
    }

}
