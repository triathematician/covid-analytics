/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
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
import java.time.Month
import kotlin.time.ExperimentalTime

class TimeSeriesAggregateTest {

    @Test
    fun testFill_Empty() {
        TimeSeriesAggregate.values().forEach {
            println(it)
            println(it.invoke(listOf(), null).values.joinToString("\t"))
            println(it.invoke(listOf(), LocalDate.of(2021, Month.DECEMBER, 21)).values.joinToString("\t"))
        }
    }

    @Test
    fun testFill_Simple() {
        val dated = listOf<Pair<LocalDate, Number>>(
                LocalDate.of(2021, Month.DECEMBER, 3) to 0,
                LocalDate.of(2021, Month.DECEMBER, 4) to 5,
                LocalDate.of(2021, Month.DECEMBER, 6) to 7,
                LocalDate.of(2021, Month.DECEMBER, 8) to 3
        )

        TimeSeriesAggregate.values().forEach {
            println(it)
            println(it.invoke(dated, null).values.joinToString("\t"))
            println(it.invoke(dated, LocalDate.of(2021, Month.DECEMBER, 21)).values.joinToString("\t"))
        }
    }

    @Test
    fun testFill_Duplicates() {
        val dated = listOf<Pair<LocalDate, Number>>(
                LocalDate.of(2021, Month.DECEMBER, 3) to 0,
                LocalDate.of(2021, Month.DECEMBER, 3) to 5,
                LocalDate.of(2021, Month.DECEMBER, 4) to 7,
                LocalDate.of(2021, Month.DECEMBER, 4) to 3
        )

        TimeSeriesAggregate.values().forEach {
            println(it)
            println(it.invoke(dated, null).values.joinToString("\t"))
            println(it.invoke(dated, LocalDate.of(2021, Month.DECEMBER, 21)).values.joinToString("\t"))
        }
    }

    @Test
    fun testFill_MissingValues() {
        val series = "331\tnull\tnull\tnull\tnull\tnull\tnull\tnull\tnull\tnull\tnull\tnull\tnull\t331\tnull\tnull\tnull\tnull\tnull\tnull\tnull"
                .split("\t").map { if (it == "null") null else it.toInt() }
        val dated = (1..21).map { LocalDate.of(2021, Month.DECEMBER, it) to series[it - 1] }
                .filter { it.second != null }
                .map { it as Pair<LocalDate, Number> }

        TimeSeriesAggregate.values().forEach {
            println(it)
            println(it.invoke(dated, null).values.joinToString("\t"))
            println(it.invoke(dated, LocalDate.of(2021, Month.DECEMBER, 21)).values.joinToString("\t"))
        }
    }

    @Test
    fun testFill_Zeros() {
        println("---------------------")
        val dated = listOf(LocalDate.of(2021, Month.DECEMBER, 1) to 0)

        TimeSeriesAggregate.values().forEach {
            println(it)
            println(it.invoke(dated, null).values.joinToString("\t"))
            println(it.invoke(dated, LocalDate.of(2021, Month.DECEMBER, 21)).values.joinToString("\t"))
        }
    }

}
