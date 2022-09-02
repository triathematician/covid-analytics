/*-
 * #%L
 * coda-data-0.4.0-SNAPSHOT
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
package tri.covid19.data

import org.junit.Test
import tri.timeseries.MetricInfo
import tri.timeseries.io.TimeSeriesFileProcessor
import tri.util.measureTime
import java.io.File

class LocalCovidDataTest {

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

        val proc0 = object : TimeSeriesFileProcessor({ JhuDailyReports.rawSources().subList(0, 2) }, { File("test3.csv") }) {
            override fun metricsProvided() = setOf(tri.covid19.CASES, tri.covid19.DEATHS).map { MetricInfo(it) }.toSet()
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
