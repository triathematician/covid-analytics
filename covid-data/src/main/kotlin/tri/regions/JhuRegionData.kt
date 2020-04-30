package tri.regions

import tri.timeseries.RegionInfo
import tri.util.csvLines

/** Loads JHU region/population data. */
object JhuRegionData {
    val data by lazy { loadData() }
    val usStates by lazy { data.filter { Fips.usState(it.fips) } }
    val usCounties by lazy { data.filter { Fips.usCounty(it.fips) } }

    private fun loadData() = JhuRegionData::class.java.getResource("resources/UID_ISO_FIPS_LookUp_Table.csv").csvLines()
                .map { JhuRegionInfo(it[0].toIntOrNull(), it[1], it[2], it[3].toIntOrNull(), it[4].toIntOrNull(),
                        it[5], it[6], it[7], it[8].toFloatOrNull(), it[9].toFloatOrNull(), it[10], it[11].toLongOrNull()) }
                .toList()

}

/** Data structure provided by JHU region data. */
class JhuRegionInfo(var uid: Int?, var iso2: String, var iso3: String, var code3: Int?, var fips: Int?,
                    var region3: String, var region2: String, var region1: String,
                    val latitude: Float?, val longitude: Float?, var combinedKey: String, val pop: Long?) {

    fun toRegionInfo() = RegionInfo(combinedKey, fips, combinedKey, pop, latitude, longitude)

}

/** Manages fips codes. */
object Fips {
    fun usState(it: Int?) = (1..99).contains(it)
    fun usCounty(it: Int?) = (1000..100000).contains(it)
}
