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
        val useId = when {
            id in UnitedStates.stateAbbreviations ->
                UnitedStates.stateFromAbbreviation(id) + ", US"
            id.removeSuffix(", US") in UnitedStates.stateAbbreviations ->
                UnitedStates.stateFromAbbreviation(id.removeSuffix(", US")) + ", US"
            else -> id
        }
        return when (val found = JhuRegionData.data[useId]) {
            null -> {
                println("Region not found: $useId")
                RegionInfo(useId, RegionType.UNKNOWN, "Unknown")
            }
            else -> found.toRegionInfo()
        }
    }

}