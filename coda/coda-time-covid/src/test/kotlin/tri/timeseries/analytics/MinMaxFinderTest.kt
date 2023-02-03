/*-
 * #%L
 * coda-data-0.4.0-SNAPSHOT
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
package tri.timeseries.analytics

@kotlin.time.ExperimentalTime
class MinMaxFinderTest {

    @org.junit.Test
    fun testFind() {
        tri.covid19.data.LocalCovidDataQuery.by({ it.type == tri.area.AreaType.PROVINCE_STATE && it.parent == tri.area.USA }, { " " !in it })
                .take(5)
                .onEach {
                    val series = it.deltas().restrictNumberOfStartingZerosTo(1).movingAverage(7)
                    println("${it.areaId} - ${it.metric} - ${series.values.map { it.toInt() }}")
                    println("  " + MinMaxFinder(10).invoke(series))
                }
    }

}
