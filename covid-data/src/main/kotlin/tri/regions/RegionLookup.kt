package tri.regions

import tri.timeseries.RegionInfo

/** Uniform lookup for region info by id. */
object RegionLookup {

    operator fun invoke(id: String): RegionInfo {
        val useId = when {
            UnitedStates.stateNames.contains(id) -> "$id, US"
            UnitedStates.countyNames.contains(id) -> "$id, US"
            else -> id
        }
        val found = JhuRegionData.data.firstOrNull { it.combinedKey == useId }
        return found?.toRegionInfo() ?: RegionInfo(useId)
    }

}