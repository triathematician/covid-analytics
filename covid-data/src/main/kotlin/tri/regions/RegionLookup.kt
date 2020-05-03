package tri.regions

import tri.timeseries.RegionInfo
import tri.timeseries.RegionType

/** Uniform lookup for region info by id. */
object RegionLookup {

    /**
     * Performs lookup on given id.
     * @param id region id
     * @param lookupUs if true, permits lookups by state name e.g. "Iowa" rather than the full id "Iowa, US"
     */
    operator fun invoke(id: String): RegionInfo {
        return when (val found = JhuRegionData.data[id]) {
            null -> {
                println("Region not found: $id")
                RegionInfo(id, RegionType.UNKNOWN, "Unknown")
            }
            else -> found.toRegionInfo()
        }
    }

}