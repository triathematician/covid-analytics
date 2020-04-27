package tri.regions

import tri.timeseries.RegionInfo

/** Uniform lookup for region info by id. */
object RegionLookup {

    operator fun invoke(id: String): RegionInfo {
        val found = JhuRegionData.data.firstOrNull { it.combinedKey == id }
        return found?.toRegionInfo() ?: RegionInfo(id)
    }

}