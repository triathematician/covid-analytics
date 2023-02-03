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

/** Information about a US county or county-equivalent. */
class UsCountyInfo(name: String, val state: UsStateInfo, fips: Int, population: Long)
    : AreaInfo(checkCountyName(name), AreaType.COUNTY, state, fips, AreaMetrics(population)) {

    val fullName: String
        get() = id

    /** Lookup CBSA corresponding to this county. */
    val cbsa
        get() = Usa.cbsaCodeByCounty[fips]?.let { Usa.cbsa(it) }

    companion object {
        /** Checks that the county name is valid. */
        internal fun checkCountyName(name: String) = when (name) {
            "District of Columbia, District of Columbia ,US" -> "District of Columbia, District of Columbia, US"
            else -> check(name, {
                it.endsWith(", US") &&
                        it.substringAfter(", ").substringBefore(", US") in UsaSourceData.statesByAbbreviation.values
            }) { "Invalid county: $it" }
        }
    }
}
