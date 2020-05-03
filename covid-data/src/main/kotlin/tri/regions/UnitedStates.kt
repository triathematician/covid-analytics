package tri.regions

import tri.timeseries.RegionInfo
import tri.util.csvKeyValues

/** Provides information about states and counties in the US. */
object UnitedStates {
    val states: List<RegionInfo> by lazy { JhuRegionData.usStates.values.map { it.toRegionInfo() } }
    val counties: List<RegionInfo> by lazy { JhuRegionData.usCounties.values.map { it.toRegionInfo() } }
    val stateNames: List<String> by lazy { states.map { it.id } }
    val countyNames: List<String> by lazy { counties.map { it.id } }
    val cbsas: List<CbsaInfo> by lazy { loadCbsaData() }

    /** Lookup CBSA for a given county. */
    fun cbsaForCounty(fips: Int) = cbsas.firstOrNull { it.counties.contains(fips) }

    private fun loadCbsaData() = UnitedStates::class.java.getResource("resources/census-cbsa-fips.csv").csvKeyValues()
            .map { CbsaInfo(it["cbsacode"]!!.toInt(), it["csacode"]?.toIntOrNull(), it["cbsatitle"]!!, it["csatitle"]!!,
                    it["cbsatitle"]!!.substringAfter(", "), listOf(it["fipsstatecode"]!!.toInt()*1000 + it["fipscountycode"]!!.toInt())) }
            .groupBy { it.cbsaCode }
            .map { it.value.first().copy(counties = it.value.flatMap { it.counties }) }
            .onEach { it.population = it.counties.sumByDouble { PopulationLookup.fips(it)?.toDouble() ?: 0.0 }.toLong() }
}

/** Manages fips codes. */
object Fips {
    fun usState(it: Int?) = (1..99).contains(it)
    fun usCounty(it: Int?) = (1000..100000).contains(it)
}

data class CbsaInfo(var cbsaCode: Int, var csaCode: Int?, var cbsaTitle: String, var csaTitle: String, var state: String,
                    var counties: List<Int>, var population: Long = 0)