package tri.area

import tri.util.csvLines

/** Region representing the world. */
val GLOBAL_AREA = JhuAreaInfo(region1 = GLOBAL, combinedKey = GLOBAL, pop = GLOBAL_POPULATION)
/** Region representing the United States. */
val US_AREA = JhuAreaInfo(840, "US", "USA", 840, null, "", "", "US", 40f, -100f, "US", 329466283)

/** Loads JHU region/population data. */
object JhuAreaData {
    val areaData by lazy { loadJhuAreaData().groupByOne { it.combinedKey } }
    val cbsaData by lazy { loadCbsaData().groupByOne { it.combinedKey } }

    val data by lazy { (areaData.values + cbsaData.values + listOf(GLOBAL_AREA)).groupByOne { it.combinedKey } }
    val usStates by lazy { areaData.filterValues { Fips.usState(it.fips) } }
    val usCounties by lazy { areaData.filterValues { Fips.usCounty(it.fips) } }
    val fipsData by lazy { areaData.filterValues { it.fips != null }.map { it.value.fips to it.value }.toMap() }

    private val lowerAreaData by lazy { areaData.mapKeys { it.key.toLowerCase() } }

    /** Looks up area by FIPS. */
    fun fips(fips: Int) = fipsData[fips]

    /** Looks up area by name, case-insensitive. */
    fun lookupCaseInsensitive(id: String) = lowerAreaData[id.trim().toLowerCase()]

    private fun loadJhuAreaData() = JhuAreaData::class.java.getResource("resources/jhu-iso-fips-lookup.csv").csvLines()
                .map { JhuAreaInfo(it[0].toIntOrNull(), it[1], it[2], it[3].toIntOrNull(), it[4].toIntOrNull(),
                        it[5], it[6], it[7], it[8].toFloatOrNull(), it[9].toFloatOrNull(), it[10], it[11].toLongOrNull()) }
                .toList()

    private fun loadCbsaData() = areaData.values
            .filter { it.fips != null && UnitedStates.countyFipsToCbsaInfo(it.fips!!) != null }
                .groupBy { UnitedStates.countyFipsToCbsaInfo(it.fips!!)!! }
                .map { it.value.toCbsa(it.key) }

    /** Combines multiple subregions into a CBSA. */
    private fun List<JhuAreaInfo>.toCbsa(cbsaInfo: CbsaInfo) = JhuAreaInfo(-1, "", "", cbsaInfo.cbsaCode, null, cbsaInfo.cbsaTitle, "",
            "US", null, null, "${cbsaInfo.cbsaTitle}, US", sumByDouble { it.pop?.toDouble() ?: 0.0 }.toLong())

}

/** Data structure provided by JHU region data. */
data class JhuAreaInfo(var uid: Int? = null, var iso2: String = "", var iso3: String = "", var code3: Int? = null, var fips: Int? = null,
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

    fun toAreaInfo() = AreaInfo(combinedKey, regionType, regionParent, fips, pop, latitude, longitude)

}

//region UTILS

private fun <X, Y> List<X>.groupByOne(map: (X) -> Y) = groupBy(map).mapValues {
    if (it.value.size > 1) {
        println("Duplicate keys: ${it.value}")
    }
    it.value.first()
}

//endregion