package tri.area

import tri.util.csvResource

/** Areas and associated data sources for USA. */
object Usa {

    private val fips = Usa::class.csvResource<CountyFips>("resources/fips.csv")
    private val stateFips = Usa::class.csvResource<StateFips>("resources/state-fips.csv")
    private val cbsaData = Usa::class.csvResource<CbsaCountyMapping>("resources/census/Mar2020cbsaDelineation.csv")

    //region LOOKUPS

    /** Mapping of US state abbreviations to state names. */
    val statesByAbbreviation = stateFips.map { it.state_abbr to it.state_name }.toMap()

    /** Mapping of US state abbreviations from state names. */
    val abbreviationsByState = stateFips.map { it.state_name to it.state_abbr }.toMap()

    /** CBSA code by county FIPS */
    val cbsaCodeByCounty = cbsaData.map { it.FIPS_County_Code to it.CBSA_Code }.toMap()

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
        it to UsRegionInfo("Region $it", statesInRegion(it))
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
    val unassigned = JhuAreaData.index
            .filterValues { it.combinedKey.startsWith("Unassigned, ") && it.combinedKey.endsWith(", US") && it.provinceOrState in stateNames }
            .map {
                it.value.combinedKey to UsCountyInfo(it.value.combinedKey, statesByLongName[it.value.provinceOrState]
                        ?: error("Invalid state: ${it.value.provinceOrState}"),
                        it.value.fips!!, it.value.population)
            }.toMap()

    /** CBSAs, indexed by CBSA Code. */
    val cbsas = cbsaData.groupBy { listOf(it.CBSA_Code, it.CSA_Code, it.CBSA_Title, it.CSA_Title) }
            .map { (info, mappings) ->
                info[0] as Int to UsCbsaInfo(info[0] as Int, info[1] as Int, info[2] as String, info[3] as String,
                        mappings.map { counties[it.fipsCombined] ?: error("County FIPS not found: ${it.FIPS_County_Code}") })
            }.toMap()
    /** CBSAs, indexed by CBSA title. */
    val cbsaByName = cbsas.mapKeys { it.value.cbsaTitle }

    //endregion

    //region LOOKUPS

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

//region US AREA TYPES

/** Information about a US region (multiple states). */
class UsRegionInfo(name: String, val states: List<UsStateInfo>)
    : AreaInfo(name, RegionType.PROVINCE_STATE_AGGREGATE, USA, null, AreaMetrics.aggregate(states))

/** Information about a US state or territory. */
class UsStateInfo(abbreviation: String, val fullName: String, fips: Int, pop: Long)
    : AreaInfo(checkState(abbreviation), RegionType.PROVINCE_STATE, USA, fips, AreaMetrics(pop))

/** Information about a US CBSA. */
class UsCbsaInfo(val cbsaCode: Int, val csaCode: Int?, val cbsaTitle: String, val csaTitle: String, val counties: List<UsCountyInfo>)
    : AreaInfo(checkCbsaTitle(cbsaTitle), RegionType.METRO, USA, cbsaCode, AreaMetrics.aggregate(counties)) {

    /** Portion of CBSA name with states. */
    private val statesText = cbsaTitle.substringAfter(",").trim()

    /** Abbreviations of all states in CBSA. */
    private val statesAbbr = statesText.splitToSequence("-").map { it.trim() }.toList()

    /** All states in CBSA. */
    val states = statesAbbr.map { Usa.state(it) }

    /** All regions in CBSA. */
    val regions = statesAbbr.map { Usa.stateFemaRegion(it) }
}

/** Information about a US county or county-equivalent. */
class UsCountyInfo(name: String, state: UsStateInfo, fips: Int, population: Long)
    : AreaInfo(checkCountyName(name), RegionType.COUNTY, state, fips, AreaMetrics(population))

/** Information about a zipcode. */
class UsZipInfo(val zipcode: Int) : AreaInfo(checkZipCode(zipcode).toString(), RegionType.ZIPCODE, null, checkZipCode(zipcode), TODO())

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

///** Common lookup information for states and counties in the US. */
//object UnitedStates {
//    val states by lazy { JhuAreaData.usStates.values.map { it.toAreaInfo() } }
//    val counties by lazy { JhuAreaData.usCounties.values.map { it.toAreaInfo() } }
//
//    val stateNames by lazy { states.map { it.id } }
//    val stateAbbreviations by lazy { stateInfo.map { it.abbr }}
//    val countyNames by lazy { counties.map { it.id } }
//    val fullCountyNames by lazy { loadFullCountyNames() }
//    val cbsas by lazy { loadCbsaData() }
//
//    /** Cache that associates county FIPS to CBSA regions. */
//    private val countyFipsToCbsaCache = mutableMapOf<Int, AreaInfo?>()
//
//    //region LOOKUPS
//
//    /** Lookup state region by name. */
//    fun state(name: String) = when (name) {
//        "U.S. Virgin Islands" -> AreaLookup("Virgin Islands, US")
//        else -> states.firstOrNull { it.id == name || it.id == "$name, US" } ?: throw IllegalArgumentException(name)
//    }
//
//    /** Lookup county by FIPS. */
//    fun fipsToCounty(fips: Int) = counties.firstOrNull { it.fips == fips }
//    /** Lookup full county name by FIPS. */
//    fun fipsToFullCountyName(fips: Int) = fullCountyNames[fips]
//    /** Lookup CBSA by FIPS. */
//    fun fipsToCbsa(fips: Int) = cbsas.firstOrNull { it.cbsaCode == fips }?.toAreaInfo()
//
//    /** Lookup CBSA for a given county. */
//    fun countyFipsToCbsaInfo(fips: Int) = cbsas.firstOrNull { it.counties.contains(fips) }
//    /** Lookup CBSA region for a given county by FIPS. */
//    fun countyFipsToCbsaRegion(fips: Int) = countyFipsToCbsaCache.getOrPut(fips) { countyFipsToCbsaInfo(fips)?.toAreaInfo() }
//
//    /** Lookup FEMA region for a given CBSA. Requires an exact match. */
//    fun stateToFemaRegion(area: AreaInfo): Int? {
//        require(area.type == RegionType.PROVINCE_STATE)
//        return femaRegion(abbreviationFromState(area.id))
//    }
//    /** Lookup FEMA region for a given CBSA. Requires an exact match. */
//    fun cbsaToCoreFemaRegion(area: AreaInfo): Int? {
//        require(area.type == RegionType.METRO)
//        return cbsas.firstOrNull { it.cbsaTitle.toLowerCase() == area.id.toLowerCase().removeSuffix(", us") }?.coreRegion
//    }
//    /** Lookup state for a given CBSA. Requires an exact match. */
//    fun cbsaToCoreState(area: AreaInfo): String? {
//        require(area.type == RegionType.METRO)
//        return cbsas.firstOrNull { it.cbsaTitle.toLowerCase() == area.id.toLowerCase().removeSuffix(", us") }?.coreState
//    }
//
//    //endregion
//
//    private fun loadFullCountyNames() = UnitedStates::class.java.getResource("resources/fips.csv").csvKeyValues()
//            .map { it.int("fips")!! to it.string("name")!! }.toMap()
//
//    private fun loadCbsaData() = UnitedStates::class.java.getResource("resources/Mar2020cbsaDelineation.csv").csvKeyValues()
//            .map { CbsaInfo(it["CBSA Code"]!!.toInt(), it["CSA Code"]?.toIntOrNull(), it["CBSA Title"]!!, it["CSA Title"]!!,
//                    it["CBSA Title"]!!.substringAfter(", "), listOf(it["FIPS Combined"]!!.toInt())) }
//            .groupBy { it.cbsaCode }
//            .map { it.value.first().copy(counties = it.value.flatMap { it.counties }) }
//            .onEach { it.population = it.counties.sumByDouble { PopulationLookup.fips(it)?.toDouble() ?: 0.0 }.toLong() }
//
//    val stateAbbrOrderedByFema = stateAbbreviations.sortedWith(compareBy({ femaRegion(it) }, { it }))
//    val stateNameOrderedByFema = stateNames.sortedWith(compareBy({ femaRegion(abbreviationFromState(it)) }, { it }))
//
//}
//
///** Manages fips codes. */
//object Fips {
//    fun usState(it: Int?) = (1..99).contains(it)
//    fun usCounty(it: Int?) = (1000..100000).contains(it)
//}