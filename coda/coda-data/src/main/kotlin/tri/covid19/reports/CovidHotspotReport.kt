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
package tri.covid19.reports

import tri.covid19.DEATHS
import tri.timeseries.*

/** Compute hotspots of given metric. */
fun List<TimeSeries>.hotspotPerCapitaInfo(metric: String = DEATHS,
                                          minPopulation: Int = 50000,
                                          maxPopulation: Int = Int.MAX_VALUE,
                                          valueFilter: (Double) -> Boolean = { it >= 5 }): List<HotspotInfo> {
    return filter { it.area.population?.let { it in minPopulation..maxPopulation } ?: true }
            .filter { it.metric == metric && valueFilter(it.lastValue) }
            .map { HotspotInfo(it.areaId, it.metric, it.start, it.values) }
}
