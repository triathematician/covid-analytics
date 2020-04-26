package triathematician.regions

import triathematician.timeseries.RegionInfo

/** Provides information about states and counties in the US. */
object UnitedStates {
    val states: List<RegionInfo> by lazy { JhuRegionData.usStates.map { it.toRegionInfo() } }
    val stateNames: List<String> by lazy { states.map { it.name } }
}