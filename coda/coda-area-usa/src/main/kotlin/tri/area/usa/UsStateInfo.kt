/*-
 * #%L
 * coda-area-usa-0.4.0-SNAPSHOT
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
package tri.area.usa

import tri.area.AreaInfo
import tri.area.AreaMetrics
import tri.area.AreaType
import tri.area.USA

/** Information about a US state or territory. */
class UsStateInfo(val abbreviation: String, val fullName: String, fips: Int, pop: Long)
    : AreaInfo(checkState(abbreviation), AreaType.PROVINCE_STATE, USA, fips, AreaMetrics(pop)) {

    val femaRegion
        get() = Usa.femaRegionByState[abbreviation]!!
    val censusRegion
        get() = Usa.censusRegionByState[abbreviation]
    val censusDivision
        get() = Usa.censusDivisionByState[abbreviation]

    companion object {
        /** Checks that the state abbreviation is valid. */
        private fun checkState(abbreviation: String) = check(abbreviation, { it in UsaSourceData.statesByAbbreviation.keys }) { "Invalid state: $it" }
    }
}
