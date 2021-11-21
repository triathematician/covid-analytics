/*-
 * #%L
 * coda-app
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
package tri.covid19.coda.data

import org.junit.Test
import tri.covid19.CASES
import tri.timeseries.TimeSeries
import tri.area.AreaType
import tri.covid19.data.LocalCovidDataQuery
import tri.timeseries.deltas
import tri.timeseries.movingAverage
import tri.util.minus
import tri.util.rangeTo
import java.io.File
import java.time.LocalDate
import kotlin.time.ExperimentalTime

class CovidDataTests {
    @Test
    @ExperimentalTime
    fun testGrowth() {
        println(File("").absolutePath)
        val data = LocalCovidDataQuery.by({ it.type == AreaType.COUNTY}, { it == CASES }).take(10)
        val latest = mutableMapOf<String, LocalDate>()
        (LocalDate.of(2020, 6, 25)..LocalDate.of(2020, 6, 30)).forEach { date ->
            val filtered = data.map { Growth(it, date) }.filter {
                it.count7 >= 100 && it.countPerCapita7 >= 20 &&
                        it.ratio730 >= .31 && (it.change7 >= 1.6 || it.change3 >= 1.6)
            }
            println("$date: ${filtered.size}")
            filtered.filter {
                val lastOn = latest.getOrElse(it.series.areaId) { LocalDate.of(2020, 1, 1) }
                val new = date.minus(lastOn) > 21
                latest.put(it.series.areaId, date)
                new
            }.let { println("  ${it.size}: ${it.map { it.series.areaId }}") }
        }
    }

    @Test
    @ExperimentalTime
    fun testKernel() {
        val data = LocalCovidDataQuery.by({ it.id == "United Kingdom"}, { it == CASES }).first()
        println(data.values)
        println(data.values.deltas().movingAverage(7))
        val kernels = data.values.deltas().movingAverage(7).windowed(6).map { bestKernel(it) }
        println(kernels)
        val kernels2 = kernels.windowed(4).map { kernelChangeRule(it) }
        println(kernels2)
    }
}

class Growth(val series: TimeSeries, val date: LocalDate) {
    val count7
        get() = series[date] - series[date-7]
    val countPerCapita7
        get() = series.area.population?.let { count7/(it/1E5) } ?: Double.NaN
    val ratio730
        get() = (series[date]-series[date-7])/(series[date]-series[date-30])
    val change7
        get() = (series[date]-series[date-7])/(series[date-7]-series[date-14])
    val change3
        get() = (series[date]-series[date-7])/(series[date-7]-series[date-14])
}

private operator fun LocalDate.minus(i: Int) = minusDays(i.toLong())

/** 0/1 are before, 2 is at, 3 is after */
fun kernelChangeRule(priors: List<Kernel>): Kernel {
    val dirs = priors.map { it.dir }
    val dirSet = dirs.toSet()
    val dirOthers = setOf(dirs[0], dirs[1], dirs[3])
    return when {
        priors[2] == Kernel.RAPID_RISE -> priors[2]
        priors[2] == Kernel.RAPID_FALL -> priors[2]
        priors[2].dir == priors[1].dir && priors[2].dir == priors[0].dir -> priors[2]
        priors[0].dir == priors[1].dir && priors[0].dir == priors[1].dir -> priors[1]
        else -> priors[0]
    }

}

fun bestKernel(vals: List<Double>): Kernel {
    val vals2 = (1 until vals.size).map { (vals[it]-vals[it-1])/(.5*vals[it]+.5*vals[it-1]) }
//    println(vals2)
    val rates = vals2.map { rate(it) }
//    println(rates)
    val mid = rates.average() + .1*rates[2]
//    println(mid)
    return when {
        mid >= 1 -> Kernel.RAPID_RISE
        mid >= .2 -> Kernel.RISE
        mid >= -.2 -> Kernel.PLATEAU
        mid >= -1 -> Kernel.FALL
        else -> Kernel.RAPID_FALL
    }
}

fun rate(d: Double) = when {
    d >= 1.0 -> 3
    d >= 0.104 -> 2
    d >= 0.0507 -> 1
    d <= -0.104 -> -2
    d <= -0.0507 -> -1
    else -> 0
}

enum class Kernel(val dir: Int) {
    RAPID_RISE(1),
    RISE(1),
    PLATEAU(0),
    FALL(-1),
    RAPID_FALL(-1)
}
