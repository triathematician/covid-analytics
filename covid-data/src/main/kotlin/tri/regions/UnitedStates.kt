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

    /** Lookup CBSA for a given county. */
    fun cbsaForCounty(fips: Int) = cbsas.firstOrNull { it.counties.contains(fips) }

    private fun loadCbsaData() = UnitedStates::class.java.getResource("resources/census-cbsa-fips.csv").csvKeyValues()
            .map { CbsaInfo(it["cbsacode"]!!.toInt(), it["csacode"]?.toIntOrNull(), it["cbsatitle"]!!, it["csatitle"]!!,
                    it["cbsatitle"]!!.substringAfter(", "), listOf(it["fipsstatecode"]!!.toInt()*1000 + it["fipscountycode"]!!.toInt())) }
            .groupBy { it.cbsaCode }
            .map { it.value.first().copy(counties = it.value.flatMap { it.counties }) }
            .onEach { it.population = it.counties.sumByDouble { PopulationLookup.fips(it)?.toDouble() ?: 0.0 }.toLong() }

    fun stateFromAbbreviation(id: String) = stateInfo.first { it.abbr.toLowerCase() == id.toLowerCase() }.let { it.name }

}

/** Manages fips codes. */
object Fips {
    fun usState(it: Int?) = (1..99).contains(it)
    fun usCounty(it: Int?) = (1000..100000).contains(it)
}

data class CbsaInfo(var cbsaCode: Int, var csaCode: Int?, var cbsaTitle: String, var csaTitle: String, var state: String,
                    var counties: List<Int>, var population: Long = 0)

data class StateInfo(var name: String, var abbr: String, var fips: Int)