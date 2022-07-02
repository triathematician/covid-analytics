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
package tri.timeseries.io

import org.junit.Test
import tri.timeseries.TimeSeries
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.test.assertEquals

@ExperimentalTime
class TimeSeriesFileFormatTest {

    @Test
    fun testSerialize() {
        assertEquals("s\ta\tm\tq\ttrue\t0\t2000-02-01\t1\t2\t3\t4",
                TimeSeriesFileFormat.writeSeriesAsString(testSeries(1, 2, 3, 4)))
        assertEquals("s\ta\tm\tq\ttrue\t0\t2000-02-01\t1\t2\t0\t0",
                TimeSeriesFileFormat.writeSeriesAsString(testSeries(1, 2, 0, 0)))
        assertEquals("s\ta\tm\tq\ttrue\t0\t2000-02-01\t0\t0\t3\t4",
                TimeSeriesFileFormat.writeSeriesAsString(testSeries(0, 0, 3, 4)))

        assertEquals("s\ta\tm\tq\tfalse\t0.0\t2000-02-01\t1.0\t2.1\t3.22\t4.333\t5.4444\t6.55555\t7.666666",
                TimeSeriesFileFormat.writeSeriesAsString(testSeries(1.0, 2.1, 3.22, 4.333, 5.4444, 6.55555, 7.666666)))

        assertEquals("s\ta\tm\tq\tfalse\t0.0\t2000-02-01\t1.0\t-Inf\t\tInf",
                TimeSeriesFileFormat.writeSeriesAsString(testSeries(1.0, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY)))
        assertEquals("s\ta\tm\tq\ttrue\t0\t2000-02-02\tInf\t0\t\t1\t",
                TimeSeriesFileFormat.writeSeriesAsString(testSeries(0, 1, 0, 0, 1, 0).div(testSeries(0, 0, 1, 0, 1, 0))))
    }

    @Test
    fun testDeserialize() {
        testRecycleSeries(1, 2, 3, 4)
        testRecycleSeries(1.0, 2.1, 3.22, 4.333, 5.4444, 6.55555, 7.666666)
        testRecycleSeries(1.0, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY)
        testSeries(0, 1, 0, 0, 1, 0).div(testSeries(0, 0, 1, 0, 1, 0)).testRecycle()
        TimeSeries("s", "a", "m", "q", true, Double.NaN, date(2, 1),
                listOf(1.0, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY)).testRecycle()
    }

    private fun date(month: Int, dayOfMonth: Int) = LocalDate.of(2000, month, dayOfMonth)

    private fun testSeries(vararg values: Int) =
            TimeSeries("s", "a", "m", "q", 0, date(2, 1), values.toList())

    private fun testSeries(vararg values: Double) =
            TimeSeries("s", "a", "m", "q", 0.0, date(2, 1), *values)

    private fun testRecycleSeries(vararg values: Int) {
        testSeries(*values).testRecycle()
    }

    private fun testRecycleSeries(vararg values: Double) {
        testSeries(*values).testRecycle()
    }

    private fun TimeSeries.testRecycle() {
        val str1 = TimeSeriesFileFormat.writeSeriesAsString(this)
        val series2 = TimeSeriesFileFormat.readSeries(str1)
        val str2 = TimeSeriesFileFormat.writeSeriesAsString(this)
        assertEquals(values, series2.values)
        assertEquals(str1, str2)
        println(str1)
    }

    private fun TimeSeries.recycleSeries() = TimeSeriesFileFormat.readSeries(TimeSeriesFileFormat.writeSeriesAsString(this))

}
