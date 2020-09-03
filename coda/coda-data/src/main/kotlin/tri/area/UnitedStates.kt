package tri.area

import tri.util.csvKeyValues
import tri.util.csvLines
import tri.util.int
import tri.util.string

/** Common lookup information for states and counties in the US. */
object UnitedStates {
    val region = AreaLookup("US")

    val states by lazy { JhuAreaData.usStates.values.map { it.toAreaInfo() } }
    val counties by lazy { JhuAreaData.usCounties.values.map { it.toAreaInfo() } }

    val stateNames by lazy { states.map { it.id } }
    val stateAbbreviations by lazy { stateInfo.map { it.abbr }}
    val countyNames by lazy { counties.map { it.id } }
    val fullCountyNames by lazy { loadFullCountyNames() }
    val cbsas by lazy { loadCbsaData() }

    val stateInfo by lazy {
        JhuAreaData::class.java.getResource("resources/state-fips.csv").csvLines()
                .map { StateInfo(it[0], it[1], it[3].toIntOrNull()!!) }
                .toList()
    }

    /** Cache that associates county FIPS to CBSA regions. */
    private val countyFipsToCbsaCache = mutableMapOf<Int, AreaInfo?>()

    //region LOOKUPS

    /** Lookup state region by name. */
    fun state(name: String) = when (name) {
        "U.S. Virgin Islands" -> AreaLookup("Virgin Islands, US")
        else -> states.firstOrNull { it.id == name || it.id == "$name, US" } ?: throw IllegalArgumentException(name)
    }

    /** Lookup county by FIPS. */
    fun fipsToCounty(fips: Int) = counties.firstOrNull { it.fips == fips }
    /** Lookup full county name by FIPS. */
    fun fipsToFullCountyName(fips: Int) = fullCountyNames[fips]
    /** Lookup CBSA by FIPS. */
    fun fipsToCbsa(fips: Int) = cbsas.firstOrNull { it.cbsaCode == fips }?.toAreaInfo()

    /** Lookup CBSA for a given county. */
    fun countyFipsToCbsaInfo(fips: Int) = cbsas.firstOrNull { it.counties.contains(fips) }
    /** Lookup CBSA region for a given county by FIPS. */
    fun countyFipsToCbsaRegion(fips: Int) = countyFipsToCbsaCache.getOrPut(fips) { countyFipsToCbsaInfo(fips)?.toAreaInfo() }

    /** Lookup FEMA region for a given CBSA. Requires an exact match. */
    fun stateToFemaRegion(area: AreaInfo): Int? {
        require(area.type == RegionType.PROVINCE_STATE)
        return femaRegion(abbreviationFromState(area.id))
    }
    /** Lookup FEMA region for a given CBSA. Requires an exact match. */
    fun cbsaToCoreFemaRegion(area: AreaInfo): Int? {
        require(area.type == RegionType.METRO)
        return cbsas.firstOrNull { it.cbsaTitle.toLowerCase() == area.id.toLowerCase().removeSuffix(", us") }?.coreRegion
    }
    /** Lookup state for a given CBSA. Requires an exact match. */
    fun cbsaToCoreState(area: AreaInfo): String? {
        require(area.type == RegionType.METRO)
        return cbsas.firstOrNull { it.cbsaTitle.toLowerCase() == area.id.toLowerCase().removeSuffix(", us") }?.coreState
    }

    fun abbreviationFromState(id: String) = stateInfo.firstOrNull { it.name.toLowerCase() == id.toLowerCase().removeSuffix(", us") }?.abbr ?: throw IllegalArgumentException("State not found: $id")
    fun stateFromAbbreviation(id: String) = stateInfo.firstOrNull { it.abbr.toLowerCase() == id.toLowerCase() }?.name ?: throw IllegalArgumentException("State abbreviation not found: $id")
    fun stateFromAbbreviationOrNull(id: String) = stateInfo.firstOrNull { it.abbr.toLowerCase() == id.toLowerCase() }?.name

    //endregion

    private fun loadFullCountyNames() = UnitedStates::class.java.getResource("resources/fips.csv").csvKeyValues()
            .map { it.int("fips")!! to it.string("name")!! }.toMap()

    private fun loadCbsaData() = UnitedStates::class.java.getResource("resources/Mar2020cbsaDelineation.csv").csvKeyValues()
            .map { CbsaInfo(it["CBSA Code"]!!.toInt(), it["CSA Code"]?.toIntOrNull(), it["CBSA Title"]!!, it["CSA Title"]!!,
                    it["CBSA Title"]!!.substringAfter(", "), listOf(it["FIPS Combined"]!!.toInt())) }
            .groupBy { it.cbsaCode }
            .map { it.value.first().copy(counties = it.value.flatMap { it.counties }) }
            .onEach { it.population = it.counties.sumByDouble { PopulationLookup.fips(it)?.toDouble() ?: 0.0 }.toLong() }

    fun femaRegion(stateAbbr: String) = when (stateAbbr) {
        "CT", "ME", "MA", "NH", "RI", "VT" -> 1
        "NJ", "NY", "PR", "VI" -> 2
        "DE", "DC", "MD", "PA", "VA", "WV" -> 3
        "AL", "FL", "GA", "KY", "MS", "NC", "SC", "TN" -> 4
        "IL", "IN", "MI", "MN", "WI", "OH" -> 5
        "NM", "LA", "TX", "AR", "OK" -> 6
        "IA", "NE", "MO", "KS" -> 7
        "CO", "MT", "ND", "SD", "UT", "WY" -> 8
        "CA", "NV", "HI", "AZ", "PI", "GU", "MP" -> 9
        "AK", "ID", "OR", "WA" -> 10
        else -> -1
    }

    val stateAbbrOrderedByFema = stateAbbreviations.sortedWith(compareBy({ femaRegion(it) }, { it }))
    val stateNameOrderedByFema = stateNames.sortedWith(compareBy({ femaRegion(abbreviationFromState(it)) }, { it }))

}

/** Manages fips codes. */
object Fips {
    fun usState(it: Int?) = (1..99).contains(it)
    fun usCounty(it: Int?) = (1000..100000).contains(it)
}

/** Information about a state. */
data class StateInfo(var name: String, var abbr: String, var fips: Int)