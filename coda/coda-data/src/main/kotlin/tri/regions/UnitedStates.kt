package tri.regions

import tri.timeseries.RegionInfo
import tri.util.csvKeyValues
import tri.util.csvLines

/** Provides information about states and counties in the US. */
object UnitedStates {
    val states: List<RegionInfo> by lazy { JhuRegionData.usStates.values.map { it.toRegionInfo() } }
    val counties: List<RegionInfo> by lazy { JhuRegionData.usCounties.values.map { it.toRegionInfo() } }

    val stateNames: List<String> by lazy { states.map { it.id } }
    val stateAbbreviations: List<String> by lazy { stateInfo.map { it.abbr }}
    val countyNames: List<String> by lazy { counties.map { it.id } }
    val cbsas: List<CbsaInfo> by lazy { loadCbsaData() }

    val stateInfo: List<StateInfo> by lazy {
        JhuRegionData::class.java.getResource("resources/state-fips.csv").csvLines()
                .map { StateInfo(it[0], it[1], it[3].toIntOrNull()!!) }
                .toList()
    }

    private val cbsaCache = mutableMapOf<Int, RegionInfo?>()

    //region LOOKUPS

    /** Lookup FIPS for county. */
    fun fipsToCounty(fips: Int) = counties.firstOrNull { it.fips == fips }

    /** Lookup CBSA for a given county. */
    fun countyFipsToCbsa(fips: Int) = cbsas.firstOrNull { it.counties.contains(fips) }
    /** Lookup CBSA region for a given county. */
    fun cbsaRegion(fips: Int) = cbsaCache.getOrPut(fips) {
        val cbsa = countyFipsToCbsa(fips) ?: return null
        JhuRegionData.cbsaRegionData["${cbsa.cbsaTitle}, US"]?.toRegionInfo()
    }

    fun abbreviationFromState(id: String) = stateInfo.first { it.name.toLowerCase() == id.removeSuffix(", US").toLowerCase() }?.abbr
    fun stateFromAbbreviation(id: String) = stateInfo.firstOrNull { it.abbr.toLowerCase() == id.toLowerCase() }?.name ?: throw IllegalArgumentException("State abbreviation not found: $id")
    fun stateFromAbbreviationOrNull(id: String) = stateInfo.firstOrNull { it.abbr.toLowerCase() == id.toLowerCase() }?.name

    //endregion

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
        "CA", "NV", "HI", "AZ", "PI" -> 9
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

data class CbsaInfo(val cbsaCode: Int, val csaCode: Int?, val cbsaTitle: String, val csaTitle: String, val state: String,
                    val counties: List<Int>, var population: Long = 0) {
    val coreState: String
        get() = state.substringBefore("-")
}

data class StateInfo(var name: String, var abbr: String, var fips: Int)