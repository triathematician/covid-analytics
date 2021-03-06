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

import org.junit.Test
import tri.covid19.data.JhuDailyReports
import tri.covid19.data.LocalCovidData
import java.io.File
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class TimeSeriesFileFormatTest {

    @Test
    fun testJson() {
        val t = TimeSeries("test", "IA", "test", "subpop", 0, LocalDate.now(), listOf(3, 1, 4, 1, 5))
        TimeSeriesFileFormat.writeSeries(t, System.out)
        val t2 = TimeSeriesFileFormat.readSeries(TimeSeriesFileFormat.writeSeriesAsString(t))
        TimeSeriesFileFormat.writeSeries(t2, System.out)
    }

    @Test
    fun testNormalize() {
        println(File("").absoluteFile)
        println(File("../data").absoluteFile)
        println(File("../data").exists())
        println(File("../../data").absoluteFile)
        println(File("../../data").exists())
        println(File("../../../data").absoluteFile)
        println(File("../../../data").exists())
        println(File("../../../../data").absoluteFile)
        println(File("../../../../data").exists())
        println(LocalCovidData.dataDir)
        println(LocalCovidData.jhuCsseProcessedData)
        val proc0 = object : TimeSeriesFileProcessor({ JhuDailyReports.rawSources() }, { File("test3.csv") }) {
            override fun inprocess(file: File) = JhuDailyReports.inprocess(file)
        }
        measureTime {
            println(proc0.data().size)
        }.also { println(it) }
        measureTime {
            println(proc0.data().size)
        }.also { println(it) }
    }

}
