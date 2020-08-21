package tri.regions

import tri.timeseries.RegionInfo
import tri.timeseries.RegionType
import tri.util.csvLines

/** Loads JHU region/population data. */
object JhuRegionData {
    val jhuRegionData by lazy { loadJhuRegionData().groupByOne { it.combinedKey } }
    val cbsaRegionData by lazy { generateCbsaRegionData().groupByOne { it.combinedKey } }

    val data by lazy { (jhuRegionData.values + cbsaRegionData.values + listOf(GLOBAL_REGION)).groupByOne { it.combinedKey } }
    val usStates by lazy { jhuRegionData.filterValues { Fips.usState(it.fips) } }
    val usCounties by lazy { jhuRegionData.filterValues { Fips.usCounty(it.fips) } }
    val fipsData by lazy { jhuRegionData.filterValues { it.fips != null }.map { it.value.fips to it.value }.toMap() }

    private fun loadJhuRegionData() = JhuRegionData::class.java.getResource("resources/jhu-iso-fips-lookup.csv").csvLines()
                .map { JhuRegionInfo(it[0].toIntOrNull(), it[1], it[2], it[3].toIntOrNull(), it[4].toIntOrNull(),
                        it[5], it[6], it[7], it[8].toFloatOrNull(), it[9].toFloatOrNull(), it[10], it[11].toLongOrNull()) }
                .toList()

    private fun generateCbsaRegionData() = jhuRegionData.values
            .filter { it.fips != null && UnitedStates.countyFipsToCbsa(it.fips!!) != null }
                .groupBy { UnitedStates.countyFipsToCbsa(it.fips!!)!! }
                .map { it.value.toCbsa(it.key) }

    fun fips(fips: Int) = fipsData[fips]

}

val GLOBAL_REGION = JhuRegionInfo(region1 = GLOBAL, combinedKey = GLOBAL, pop = GLOBAL_POPULATION)
val US_REGION = JhuRegionInfo(840, "US", "USA", 840, null, "", "", "US",
        40f, -100f, "US", 329466283)

private fun <X, Y> List<X>.groupByOne(map: (X) -> Y) = groupBy(map).mapValues {
    if (it.value.size > 1) {
        println("Duplicate keys: ${it.value}")
    }
    it.value.first()
}

private fun List<JhuRegionInfo>.toCbsa(cbsaInfo: CbsaInfo) = JhuRegionInfo(-1, "", "", cbsaInfo.cbsaCode, null, cbsaInfo.cbsaTitle, "",
            "US", null, null, "${cbsaInfo.cbsaTitle}, US", sumByDouble { it.pop?.toDouble() ?: 0.0 }.toLong())

/** Data structure provided by JHU region data. */
data class JhuRegionInfo(var uid: Int? = null, var iso2: String = "", var iso3: String = "", var code3: Int? = null, var fips: Int? = null,
                    var region3: String = "", var region2: String = "", var region1: String,
                    val latitude: Float? = null, val longitude: Float? = null, var combinedKey: String, val pop: Long? = null) {

    val regionType
        get() = when {
            region1 == GLOBAL -> RegionType.GLOBAL
            region2.isEmpty() && region3.isEmpty() -> RegionType.COUNTRY_REGION
            region2.isEmpty() -> RegionType.METRO
            region3.isEmpty() -> RegionType.PROVINCE_STATE
            else -> RegionType.COUNTY
        }

    val regionParent
        get() = when {
            region1 == GLOBAL -> ""
            region2.isEmpty() && region3.isEmpty() -> GLOBAL
            region2.isEmpty() -> region1
            region3.isEmpty() -> region1
            else -> "$region2, $region1"
        }

    fun toRegionInfo() = RegionInfo(combinedKey, regionType, regionParent, fips, pop, latitude, longitude)

}
