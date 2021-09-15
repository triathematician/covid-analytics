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
package tri.timeseries

import junit.framework.Assert.assertEquals
import org.junit.Test
import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.covid19.data.JhuDailyReports
import tri.covid19.data.LocalCovidData
import java.io.File
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import tri.util.plus

@ExperimentalTime
class TimeSeriesTest {

    @Test
    fun testAverage() {
        val values = listOf(1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2)
        val t = TimeSeries("test", "IA", "test", "subpop", 0, LocalDate.now(), values)

        assertEquals(listOf(0.0, 0.0, 1.0, 0.0, 0.0, 0.5, 0.0, 0.0, -1.0, Double.NaN, Double.NaN, Double.POSITIVE_INFINITY, 0.0, 0.0, 1.0, 0.0, 0.0),
            t.percentChanges(1, 1).values)
        assertEquals(listOf(0.5, 1.0, 0.3333333333333333, 0.25, 0.5, 0.2, -0.5, -1.0, -1.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0, 0.5, 1.0, 0.3333333333333333),
            t.percentChanges(2, 2).values)
        assertEquals(listOf(-0.3333333333333333, -0.5714285714285714, -0.6874999999999999, -0.5333333333333333, -0.30769230769230765),
            t.percentChanges(7, 7).values)

        assertEquals(LocalDate.now() + 13, t.percentChanges(7, 7).start)
    }

}
