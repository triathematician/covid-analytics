package tri.area

import tri.timeseries.TimeSeries
import tri.timeseries.sum
import tri.util.csvResource

/** Areas and associated data sources for USA. */
object Usa {

    private val fips = Usa::class.csvResource<CountyFips>(true, "resources/fips.csv")
    private val stateFips = Usa::class.csvResource<StateFips>(true, "resources/state-fips.csv")
    private val cbsaData = Usa::class.csvResource<CbsaCountyMapping>(true, "resources/census/Mar2020cbsaDelineation.csv")

    //region LOOKUP TABLES

    /** Mapping of US state abbreviations to state names. */
    val statesByAbbreviation = stateFips.map { it.state_abbr to it.state_name }.toMap()
    /** Mapping of US state abbreviations from state names. */
    val abbreviationsByState = stateFips.map { it.state_name to it.state_abbr }.toMap()
    /** CBSA code by county FIPS */
    val cbsaCodeByCounty = cbsaData.map { it.fipsCombined to it.CBSA_Code }.toMap()

    //endregion

    //region MASTER AREA INDICES

    /** US states, indexed by abbreviation. */
    val states = JhuAreaData.index.filter { it.key is String && it.value.fips != null && it.value.fips!! < 100 }
            .map { it.key as String to UsStateInfo(it.key as String, it.value.provinceOrState, it.value.fips!!, it.value.population) }
            .toMap()

    /** List of US state abbreviations. */
    val stateAbbreviations = stateFips.map { it.state_abbr }
    /** List of US state names, e.g. "Ohio". */
    val stateNames = states.map { it.value.fullName }

    /** US states, indexed by full name, e.g. "Ohio". */
    val statesByLongName = states.mapKeys { it.value.fullName }

    /** FEMA regions, indexed by number. */
    val femaRegions = (1..10).map {
        it to UsRegionInfo("Region $it", it, statesInRegion(it))
    }.toMap()

    /** FEMA regions by state */
    val femaRegionByState = stateFips.map { it.state_abbr to (femaRegions[it.fema_region] ?: error("Region!")) }.toMap()

    /** Counties, indexed by FIPS. */
    val counties = JhuAreaData.index.filterValues { validCountyFips(it.fips) }
            .map {
                it.value.fips!! to UsCountyInfo(it.value.combinedKey, statesByLongName[it.value.provinceOrState]
                        ?: error("Invalid state: ${it.value.provinceOrState}"),
                        it.value.fips!!, it.value.population)
            }.toMap()

    /** Unassigned regions, indexed by state. */
    val unassigned = states
            .map { it.value.abbreviation to UsCountyInfo("Unassigned, ${it.value.fullName}, US", it.value, it.value.fips!!*1000, 0L) }
            .toMap()

    /** CBSAs, indexed by CBSA Code. */
    val cbsas = cbsaData.groupBy { listOf(it.CBSA_Code, it.CSA_Code, it.CBSA_Title, it.CSA_Title) }
            .map { (info, mappings) ->
                info[0] as Int to UsCbsaInfo(info[0] as Int, info[1] as Int, info[2] as String, info[3] as String,
                        mappings.map { counties[it.fipsCombined] ?: error("County FIPS not found: ${it.FIPS_County_Code}") })
            }.toMap()
    /** CBSAs, indexed by CBSA title. */
    val cbsaByName = cbsas.mapKeys { it.value.cbsaTitle }

    //endregion

    //region COUNTY LOOKUPS

    /** Get unassigned county b GIPS. */
    fun unassignedCounty(fips: Int): UsCountyInfo? {
        val stateF = if (fips < 1000) fips else fips/1000
        val state = stateFips.firstOrNull { it.fips == stateF }?.state_abbr ?: return null
        return unassigned[state]
    }

    /** Get county for the given FIPS. */
    fun county(fips: Int) = counties[fips]

    /** Get county by a given name. */
    fun county(name: String) = checkCountyName(name).let { Lookup.area(name) as? UsCountyInfo }

    //endregion

    //region CBSA LOOKUPS

    /** Get CBSA by given code. */
    fun cbsa(code: Int) = cbsas[code]

    /** Get CBSA by given name. */
    fun cbsa(name: String) = cbsaByName[name]

    //endregion

    //region STATE LOOKUPS

    /** Lookup state by long name. */
    fun stateByLongName(name: String) = statesByLongName[name] ?: error("Invalid state name $name")

    /** Lookup state by abbreviation. */
    fun state(abbrev: String) = states[abbrev] ?: error("Invalid state abbreviation $abbrev")

    /** Lookup FEMA region by abbreviation. */
    fun stateFemaRegion(abbrev: String) = femaRegionByState[abbrev] ?: error("Invalid state abbreviation $abbrev")

    //endregions

    //region UTILS

    private fun validCountyFips(n: Int?) = n != null && n >= 1000 && n < 80000 && n % 1000 != 0

    private fun statesInRegion(n: Int) = stateFips.filter { it.fema_region == n }.map {
        states[it.state_abbr] ?: error("State!")
    }

    //endregion
}

//region SERIES PROCESSORS

val List<TimeSeries>.counties
    get() = filter { it.area is UsCountyInfo }
val List<TimeSeries>.cbsas
    get() = filter { it.area is UsCbsaInfo }
val List<TimeSeries>.states
    get() = filter { it.area is UsStateInfo }
val List<TimeSeries>.national
    get() = filter { it.area == USA }

/** Adds rollups of series to a list of time series. Does not check that the input data is at the proper level. */
fun List<TimeSeries>.withAggregate(cbsa: Boolean = false, state: Boolean = false, national: Boolean = false): List<TimeSeries> {
    val res = mutableListOf(this)
    if (cbsa) res += aggregateByCbsa().flatMap { it.value }
    if (state) res += aggregateByState().flatMap { it.value }
    if (national) res += aggregateToNational()
    return res.flatten()
}

/** Sums metric data associated with counties and aggregates to CBSA by summing. Assumes time series are US county info. */
fun List<TimeSeries>.aggregateByCbsa(): Map<Int, List<TimeSeries>> {
    return groupBy { listOf(it.source, (it.area as? UsCountyInfo)?.cbsa, it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsCbsaInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { it.area.fips!! }
}

/** Sums metric data associated with counties and aggregates to state by summing. Assumes time series are US county info. */
fun List<TimeSeries>.aggregateByState(): Map<String, List<TimeSeries>> {
    return groupBy { listOf(it.source, (it.area as? UsCountyInfo)?.state, it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsStateInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { (it.area as UsStateInfo).abbreviation }
}

/** Sums metric data and aggregates to USA national. Assumes time series are disjoint areas covering the USA. */
fun List<TimeSeries>.aggregateToNational() = groupBy { listOf(it.source, it.metric, it.qualifier) }.map { it.value.sum(USA.id) }

//endregion

//region US AREA TYPES

/** Information about a US region (multiple states). */
class UsRegionInfo(name: String, val num: Int, val states: List<UsStateInfo>)
    : AreaInfo(name, AreaType.PROVINCE_STATE_AGGREGATE, USA, null, AreaMetrics.aggregate(states))

/** Information about a US state or territory. */
class UsStateInfo(val abbreviation: String, val fullName: String, fips: Int, pop: Long)
    : AreaInfo(checkState(abbreviation), AreaType.PROVINCE_STATE, USA, fips, AreaMetrics(pop)) {

    val femaRegion
        get() = Usa.femaRegionByState[abbreviation]!!
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

    /** All regions in CBSA. */
    val regions = statesAbbr.map { Usa.stateFemaRegion(it) }

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

//endregion

//region CHECKERS

/** Checks that the state abbreviation is valid. */
private fun checkState(abbreviation: String) = check(abbreviation, { it in Usa.statesByAbbreviation.keys }) { "Invalid state: $it" }

/** Checks that the CBSA title is valid. */
private fun checkCbsaTitle(title: String) = check(title, { it.contains(", ") }) { "Invalid CBSA title: $it" }

/** Checks that the county name is valid. */
private fun checkCountyName(name: String) = when (name) {
    "District of Columbia, District of Columbia ,US" -> "District of Columbia, District of Columbia, US"
    else -> check(name, {
        it.endsWith(", US") &&
                it.substringAfter(", ").substringBefore(", US") in Usa.statesByAbbreviation.values
    }) { "Invalid county: $it" }
}

/** Checks that the zipcode is valid. */
private fun checkZipCode(code: Int) = check(code, { it in 0..99999 }) { "Invalid zipcode: $it" }

/** Utility method for inline checking of values. */
private fun <X> check(x: X, test: (X) -> Boolean, message: (X) -> String): X = if (test(x)) x else throw IllegalArgumentException(message(x))

//endregion

//region DATA LOADER CLASSES

/** Mapping for state FIPS file. */
class StateFips(val state_name: String, val state_abbr: String, val long_name: String,
                val fips: Int, val sumlev: Int, val region: Int, val division: Int, val state: Int,
                val region_name: String, val division_name: String, val fema_region: Int)

/** Mapping for FIPS file. */
class CountyFips(val fips: Int, val name: String, val state: String)

/** Mapping for CBSA delineation file. */
class CbsaCountyMapping(val CBSA_Code: Int, val Metropolitan_Division_Code: String, val CSA_Code: Int,
                        val CBSA_Title: String, val CBSA_Type: String,
                        val Metropolitan_Division_Title: String, val CSA_Title: String,
                        val County_or_Equivalent: String, val State_Name: String,
                        val FIPS_State_Code: Int, val FIPS_County_Code: Int, val FIPS_Combined: Int,
                        val Central_Outlying_County: String) {
    val fipsCombined = FIPS_State_Code * 1000 + FIPS_County_Code
}

//endregion