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
package tri.area

/** Metrics associated with a geographic region. */
class AreaMetrics(
        val population: Long? = null,
        val latitude: Float? = null,
        val longitude: Float? = null) {

    companion object {
        fun aggregate(areas: List<AreaInfo>) = AreaMetrics(areas.totalPopulation)
    }
}

/** Sums area populations. */
val List<AreaInfo>.totalPopulation
        get() = sumByDouble { it.population?.toDouble() ?: 0.0 }.toLong()
