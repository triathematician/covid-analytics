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
package tri.area.usa

import com.fasterxml.jackson.annotation.JsonProperty
import tri.area.AreaInfo
import tri.area.AreaMetrics
import tri.area.AreaType
import tri.area.EARTH

/** Loads JHU region/population data. */
object JhuAreaData {
    val index = UsaSourceData.jhuData.groupByOne { it.indexKey }
    val areas = index.values
    val usCounties = UsaSourceData.jhuData.filter { validCountyFips(it.fips) }.associateBy { it.fips!! }

    private val lowerIndex by lazy { index.mapKeys { it.key.toString().toLowerCase() } }

    /** Looks up area by name, case-insensitive. */
    fun lookupCaseInsensitive(key: Any) = index.getOrElse(key) { lowerIndex[key.toString().trim().toLowerCase()] }
}

/** Data structure provided by JHU region data. */
data class JhuAreaInfo(val UID: Int, val iso2: String, val iso3: String, var code3: Int,
                       @JsonProperty("FIPS") val fips: Int? = null, @JsonProperty("Admin2") val admin2: String,
                       @JsonProperty("Province_State") val provinceOrState: String, @JsonProperty("Country_Region") val countryOrRegion: String,
                       @JsonProperty("Lat") val latitude: Float, @JsonProperty("Long_") val longitude: Float,
                       @JsonProperty("Combined_Key") val combinedKey: String, @JsonProperty("Population") val population: Long) {

    /** Get unique key used to lookup this region. Regions with FIPS have more than one possible key. */
    val indexKey: List<Any>
        get() = when {
            fips == null -> listOf(combinedKey)
            fips < 100 -> listOf(fips, abbreviationsByState[provinceOrState]!!)
            else -> listOf(fips, combinedKey)
        }

    val regionType
        get() = when {
            admin2.isEmpty() && provinceOrState.isEmpty() -> AreaType.COUNTRY_REGION
            provinceOrState.isEmpty() -> AreaType.METRO
            admin2.isEmpty() -> AreaType.PROVINCE_STATE
            else -> AreaType.COUNTY
        }

    val regionParent
        get() = when {
            admin2.isEmpty() && provinceOrState.isEmpty() -> EARTH
            provinceOrState.isEmpty() || admin2.isEmpty() -> UsaAreaLookup.areaOrNull(countryOrRegion)!!
            else -> UsaAreaLookup.areaOrNull("$provinceOrState, $countryOrRegion")!!
        }

    /** Convert to general area info object. */
    fun toAreaInfo(): AreaInfo {
        require(fips == null || fips >= 80000) { "Use Usa object to access areas within the US: $this" }
        return AreaInfo(combinedKey, regionType, regionParent, fips, AreaMetrics(population, latitude, longitude))
    }

    companion object {
        private val abbreviationsByState = UsaSourceData.stateFips.associate { it.state_name to it.state_abbr }
    }
}
