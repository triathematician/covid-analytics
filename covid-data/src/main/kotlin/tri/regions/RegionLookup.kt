package tri.regions

import tri.timeseries.RegionInfo

/** Uniform lookup for region info by id. */
object RegionLookup {

    /**
     * Performs lookup on given id.
     * @param id region id
     * @param lookupUs if true, permits lookups by state name e.g. "Iowa" rather than the full id "Iowa, US"
     */
    operator fun invoke(id: String, lookupUs: Boolean = true): RegionInfo {
        val useId = when {
            !lookupUs -> id
            UnitedStates.stateNames.contains(id) -> "$id, US"
            UnitedStates.countyNames.contains(id) -> "$id, US"
            else -> id
        }
        val found = JhuRegionData.data.firstOrNull { it.combinedKey == useId }
        return found?.toRegionInfo() ?: RegionInfo(useId)
    }

}