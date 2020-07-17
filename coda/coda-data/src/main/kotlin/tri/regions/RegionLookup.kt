package tri.regions

import tri.timeseries.RegionInfo
import tri.timeseries.RegionType

/** Uniform lookup for region info by id. */
object RegionLookup {

    /**
     * Performs lookup on given id.
     * @param id region id
     */
    operator fun invoke(id: String): RegionInfo {
        val useId = when {
            id == "District of Columbia, District of Columbia, US" -> UnitedStates.stateFromAbbreviation("DC") + ", US"
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