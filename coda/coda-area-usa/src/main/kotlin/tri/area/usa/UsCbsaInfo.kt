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

/** Information about a US CBSA. */
data class UsCbsaInfo(val cbsaCode: Int, val csaCode: Int?, val cbsaTitle: String, val csaTitle: String, val counties: List<UsCountyInfo>)
    : AreaInfo(checkCbsaTitle(cbsaTitle), AreaType.METRO, USA, cbsaCode, AreaMetrics.aggregate(counties)) {

    /** Portion of CBSA name with states. */
    private val statesText = cbsaTitle.substringAfter(",").trim()

    /** Abbreviations of all states in CBSA. */
    private val statesAbbr = statesText.splitToSequence("-").map { it.trim() }.toList()

    /** All states in CBSA. */
    val states = statesAbbr.map { Usa.state(it) }

    /** Get list of county FIPS numbers in this CBSA. */
    val countyFips
        get() = counties.map { it.fips!! }

    companion object {
        /** Checks that the CBSA title is valid. */
        private fun checkCbsaTitle(title: String) = check(title, { it.contains(", ") }) { "Invalid CBSA title: $it" }
    }
}
