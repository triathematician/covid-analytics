package tri.regions

import tri.timeseries.RegionInfo
import tri.util.csvKeyValues

/** Provides information about states and counties in the US. */
object UnitedStates {
    val states: List<RegionInfo> by lazy { JhuRegionData.usStates.map { it.toRegionInfo() } }
    val counties: List<RegionInfo> by lazy { JhuRegionData.usCounties.map { it.toRegionInfo() } }
    val stateNames: List<String> by lazy { states.map { it.name.removeSuffix(", US") } }
    val countyNames: List<String> by lazy { counties.map { it.name.removeSuffix((", US")) } }

    val cbsas: List<CbsaInfo> by lazy { loadCbsaData() }

    /** Lookup CBSA for a given county. */
    fun cbsa(fips: Int) = cbsas.firstOrNull { it.counties.contains(fips) }

    private fun loadCbsaData() = UnitedStates::class.java.getResource("resources/census-cbsa-fips.csv").csvKeyValues()
            .map { CbsaInfo(it["cbsacode"]!!.toInt(), it["csacode"]?.toIntOrNull(), it["cbsatitle"]!!, it["csatitle"],
                    listOf(it["fipsstatecode"]!!.toInt()*1000 + it["fipscountycode"]!!.toInt())) }
            .groupBy { it.cbsaCode }
            .map { it.value.first().copy(counties = it.value.flatMap { it.counties }) }
}

data class CbsaInfo(var cbsaCode: Int, var csaCode: Int?, var cbsaTitle: String, var csaTitle: String?, var counties: List<Int>)