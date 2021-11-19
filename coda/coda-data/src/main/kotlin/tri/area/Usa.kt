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
package tri.area

import tri.area.UsaSourceData.cbsaData
import tri.area.UsaSourceData.stateFips


/** Areas and associated data sources for USA. */
object Usa {

    //region CORE STATE/REGION INDICES (must be initialized first)

    /** US states, indexed by abbreviation. */
    val states = JhuAreaData.index.filter { it.key is String && it.value.fips != null && it.value.fips!! < 100 }
        .map { it.key as String to UsStateInfo(it.key as String, it.value.provinceOrState, it.value.fips!!, it.value.population) }
        .toMap()

    /** State area objects. */
    val stateAreas = states.values.sortedBy { it.id }
    /** List of US state names, e.g. "Ohio". */
    val stateNames = states.map { it.value.fullName }

    /** US states, indexed by full name, e.g. "Ohio". */
    val statesByLongName = states.mapKeys { it.value.fullName }

    /** FEMA regions, indexed by number. */
    val femaRegions = UsaSourceData.stateFips.groupBy { it.fema_region }
        .map { (num, states) -> num to region("Region $num", states) }
        .toMap()

    //endregion

    //region MASTER AREA INDICES

    /** Counties, indexed by FIPS. */
    val counties = UsaSourceData.jhuData.groupByOne { it.indexKey }.filterValues { validCountyFips(it.fips) }
        .map {
            it.value.fips!! to UsCountyInfo(it.value.combinedKey, statesByLongName[it.value.provinceOrState]
                ?: error("Invalid state: ${it.value.provinceOrState}"),
                it.value.fips!!, it.value.population)
        }.toMap()

    /** CBSAs, indexed by CBSA Code. */
    val cbsas = cbsaData.groupBy { listOf(it.CBSA_Code, it.CSA_Code, it.CBSA_Title, it.CSA_Title) }
        .map { (info, mappings) ->
            info[0] as Int to UsCbsaInfo(info[0] as Int, info[1] as Int, info[2] as String, info[3] as String,
                mappings.map {
                    counties[it.fipsCombined] ?: error("County FIPS not found: ${it.FIPS_County_Code}")
                })
        }.toMap()

    /** Census regions and divisions, indexed by name. */
    val censusRegions = stateFips.filter { it.region_name.isNotEmpty() }.groupBy { it.region_name }.map { (name, states) -> name to region(name, states) }.toMap() +
            stateFips.filter { it.division_name.isNotEmpty() }.groupBy { it.division_name }.map { (name, states) -> name to region(name, states) }.toMap()
    /** Census regions by state */
    val censusRegionByState = stateFips.filter { it.region_name.isNotEmpty() }
        .associate { it.state_abbr to censusRegions[it.region_name]!! }
    /** Census divisions by state */
    val censusDivisionByState = stateFips.filter { it.division_name.isNotEmpty() }
        .associate { it.state_abbr to censusRegions[it.division_name]!! }

    /** Region X. */
    val regionX = UsRegionInfo("X", "WA,OR,CA,NV,AZ,HI,CO,NM,MN,WI,IL,MI,ME,NH,VT,NY,PA,VA,GA,MA,RI,CT,NJ,DE,MD,DC".split(",").map { states[it]!! })
    val regionY = UsRegionInfo("Y", "AK,ID,MT,WY,UT,ND,SD,NE,KS,OK,TX,IA,MO,AR,LA,IN,OH,KY,WV,TN,MS,AL,NC,SC,FL".split(",").map { states[it]!! })

    //endregion

    //region AREA LISTS

    /** County area objects. */
    val countyAreas = counties.values.toList()

    /** CBSA area objects. */
    val cbsaAreas = cbsas.values.toList()

    /** Ordered FEMA regions. */
    val femaRegionAreas = (1..10).map { femaRegions[it]!! }
    /** Census region areas. */
    val censusRegionAreas = censusRegionByState.values.toSet()
    /** Census division areas. */
    val censusDivisionAreas = censusDivisionByState.values.toSet()

    /** All regional area groupings. */
    val allRegionAreas = femaRegionAreas + censusRegionAreas + censusDivisionAreas

    //endregion

    //region LOOKUP TABLES

    /** Unassigned regions, indexed by state. */
    val unassignedCountiesByState = states
        .map { it.value.abbreviation to UsCountyInfo("Unassigned, ${it.value.fullName}, US", it.value, it.value.fips!! * 1000, 0L) }
        .toMap()

    /** CBSAs, indexed by CBSA title. */
    val cbsaByName = cbsas.mapKeys { it.value.cbsaTitle }
    /** CBSA's by county */
    val cbsaByCounty = cbsas.values.flatMap { cbsa -> cbsa.counties.map { it to cbsa } }.toMap()
    /** CBSA code by county FIPS */
    val cbsaCodeByCounty = cbsaData.associate { it.fipsCombined to it.CBSA_Code }

    /** FEMA regions by state */
    val femaRegionByState = stateFips.associate { it.state_abbr to (femaRegions[it.fema_region] ?: error("Region!")) }

    //endregion

    //region LOOKUP FUNCTIONS

    /** Get county for the given FIPS. */
    fun county(fips: Int) = counties[fips]
    /** Get county by a given name. */
    fun county(name: String) = checkCountyName(name).let { Lookup.area(name) as? UsCountyInfo }

    /** Get unassigned county by FIPS. */
    fun unassignedCounty(fips: Int): UsCountyInfo? {
        val stateF = if (fips < 1000) fips else fips / 1000
        val state = stateFips.firstOrNull { it.fips == stateF }?.state_abbr ?: return null
        return unassignedCountiesByState[state]
    }

    /** Get CBSA by given code. */
    fun cbsa(code: Int) = cbsas[code]
    /** Get CBSA by given name. */
    fun cbsa(name: String) = cbsaByName[name]

    /** Lookup state by abbreviation. */
    fun state(abbrev: String) = states[abbrev] ?: error("Invalid state abbreviation $abbrev")
    /** Lookup state by long name. */
    fun stateByLongName(name: String) = statesByLongName[name] ?: error("Invalid state name $name")

    /** Lookup FEMA region by abbreviation. */
    fun femaRegionByState(abbrev: String) = femaRegionByState[abbrev]

    /** Get all regions (multiple types) that contain this state. */
    val regionsByState = stateAreas.associateWith { state -> allRegionAreas.filter { state in it.states }.toSet() }
    /** Get all regions (multiple types) that contain this region. Includes itself. */
    val regionsByRegion = allRegionAreas.associateWith { region -> setOf(region) + allRegionAreas.filter { it.states.containsAll(region.states) } }

    //endregion
}

//region UTILS

internal fun validCountyFips(n: Int?) = n != null && n >= 1000 && n < 80000 && n % 1000 != 0

private fun region(name: String, states: List<UsaSourceData.StateFips>) = UsRegionInfo(name, states.mapNotNull {
    Usa.states[it.state_abbr]
})

internal fun <X, Y> List<X>.groupByOne(keySelectors: (X) -> List<Y>): Map<Y, X> {
    val res = mutableMapOf<Y, MutableList<X>>()
    for (element in this) {
        keySelectors(element).forEach { key ->
            val list = res.getOrPut(key) { ArrayList() }
            list.add(element)
        }
    }
    res.values.forEach { if (it.size > 1) println("Two values had the same key: $it") }
    return res.mapValues { it.value.first() }
}

//endregion

//region US AREA TYPES

/** Information about a US region (multiple states). */
class UsRegionInfo(name: String, val states: List<UsStateInfo>)
    : AreaInfo(name, AreaType.PROVINCE_STATE_AGGREGATE, USA, null, AreaMetrics.aggregate(states))

/** Information about a US state or territory. */
class UsStateInfo(val abbreviation: String, val fullName: String, fips: Int, pop: Long)
    : AreaInfo(checkState(abbreviation), AreaType.PROVINCE_STATE, USA, fips, AreaMetrics(pop)) {

    val femaRegion
        get() = Usa.femaRegionByState[abbreviation]!!
    val censusRegion
        get() = Usa.censusRegionByState[abbreviation]
    val censusDivision
        get() = Usa.censusDivisionByState[abbreviation]
}

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
}

/** Information about a US county or county-equivalent. */
class UsCountyInfo(name: String, val state: UsStateInfo, fips: Int, population: Long)
    : AreaInfo(checkCountyName(name), AreaType.COUNTY, state, fips, AreaMetrics(population)) {

    val fullName: String
        get() = id

    /** Lookup CBSA corresponding to this county. */
    val cbsa
        get() = Usa.cbsaCodeByCounty[fips]?.let { Usa.cbsa(it) }
}

/** Information about a zipcode. */
class UsZipInfo(val zipcode: Int) : AreaInfo(checkZipCode(zipcode).toString(), AreaType.ZIPCODE, null, checkZipCode(zipcode), TODO())

/** Information about an HSA (hospital service area). */
class UsHsaInfo(val num: Int, val name: String, customId: String? = null, customPop: Long? = null) :
    AreaInfo(customId ?: "HSA_${num}", AreaType.UNKNOWN, USA, null, AreaMetrics(population = customPop))

/** Information about an HSA (hospital service area). */
class UsHrrInfo(val num: Int, val name: String) : AreaInfo("HRR_${num}", AreaType.UNKNOWN, USA, null, AreaMetrics())

/** Gets a friendly name for areas that are within the USA. */
val AreaInfo.friendlyName
    get() = when (this) {
        is UsStateInfo -> fullName
        is UsCountyInfo -> fullName
        is UsCbsaInfo -> cbsaTitle
        is UsHrrInfo -> name
        is UsHsaInfo -> name
        else -> id
    }

/** Get all ancestors of the given area, including itself. */
fun AreaInfo.ancestors(): Set<AreaInfo> = when (this) {
    USA -> setOf(USA)
    is UsRegionInfo -> Usa.regionsByRegion[this]!! + USA
    is UsStateInfo -> setOf(this) + Usa.regionsByState[this]!!.flatMap { Usa.regionsByRegion[it]!! }
    is UsCbsaInfo -> setOf(this) + states.flatMap { it.ancestors() }.toSet()
    is UsCountyInfo -> setOfNotNull(this, Usa.cbsaByCounty[this]) + state.ancestors()
    else -> setOf(this)
} + USA

//endregion

//region CHECKERS

/** Checks that the state abbreviation is valid. */
private fun checkState(abbreviation: String) = check(abbreviation, { it in UsaSourceData.statesByAbbreviation.keys }) { "Invalid state: $it" }

/** Checks that the CBSA title is valid. */
private fun checkCbsaTitle(title: String) = check(title, { it.contains(", ") }) { "Invalid CBSA title: $it" }

/** Checks that the county name is valid. */
private fun checkCountyName(name: String) = when (name) {
    "District of Columbia, District of Columbia ,US" -> "District of Columbia, District of Columbia, US"
    else -> check(name, {
        it.endsWith(", US") &&
                it.substringAfter(", ").substringBefore(", US") in UsaSourceData.statesByAbbreviation.values
    }) { "Invalid county: $it" }
}

/** Checks that the zipcode is valid. */
private fun checkZipCode(code: Int) = check(code, { it in 0..99999 }) { "Invalid zipcode: $it" }

/** Utility method for inline checking of values. */
private fun <X> check(x: X, test: (X) -> Boolean, message: (X) -> String): X = if (test(x)) x else throw IllegalArgumentException(message(x))

//endregion
