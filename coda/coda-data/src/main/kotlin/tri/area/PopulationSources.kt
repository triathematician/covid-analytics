/*-
 * #%L
 * coda-data
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
package tri.area

import tri.util.csvLines

//
// This file provides access to population data tables.
//

private const val COUNTY_CENSUS_DATA = "resources/census/county census.csv"
private const val STATE_CENSUS_DATA = "resources/census/state census.csv"
private const val METRO_DATA = "resources/metro.csv"
private const val COUNTRY_DATA = "resources/countries.csv"

sealed class PopulationLookupData(resource: String): (String) -> Long? {
    val dataLines = CountyData::class.java.getResource(resource).csvLines(true).toList()
    val dataTable: MutableMap<String, Long> = mutableMapOf()
}

object CountyData: PopulationLookupData(COUNTY_CENSUS_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[12].replace(",", "").toLong() } }

    override fun invoke(input: String): Long? {
        val split = input.split(", ", ",")
        if (split.size != 2 && split.size != 3) {
            return null
        }

        // account for all the possible suffices for counties in the US
        return dataTable[input.removeSuffix(", US").toLowerCase()]
                ?: dataTable["${split[0]}, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} county, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} parish, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} city, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} municipality, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} borough, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} city and borough, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} census area, ${split[1]}".toLowerCase()]
    }
}

object StateData: PopulationLookupData(STATE_CENSUS_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[12].replace(",", "").toLong() } }
    override fun invoke(input: String) = when (input.split(", ", ",").size) {
        2 -> dataTable[input.removeSuffix(", US").toLowerCase()]
        1 -> dataTable[input.toLowerCase()]
        else -> null
    }
}

object MetroData: PopulationLookupData(METRO_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[1].toLong() } }
    override fun invoke(input: String) = dataTable[input.toLowerCase()]
}

object CountryData: PopulationLookupData(COUNTRY_DATA) {
    init { dataLines.forEach { dataTable[it[1].toLowerCase()] = it[2].replace(",", "").toLong() } }

    override fun invoke(input: String) = when {
        input.endsWith(", France") -> dataTable[input.removeSuffix(", France").toLowerCase()]
        input.endsWith(", United Kingdom") -> dataTable[input.removeSuffix(", United Kingdom").toLowerCase()]
        input.endsWith(", Netherlands") -> dataTable[input.removeSuffix(", Netherlands").toLowerCase()]
        input.endsWith(", Denmark") -> dataTable[input.removeSuffix(", Denmark").toLowerCase()]
        input.endsWith(", US") -> dataTable[input.removeSuffix(", US").toLowerCase()]
        else -> dataTable[input.toLowerCase()]
    }
}
