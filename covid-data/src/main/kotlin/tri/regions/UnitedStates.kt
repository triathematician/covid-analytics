package tri.regions

import tri.timeseries.RegionInfo

/** Provides information about states and counties in the US. */
object UnitedStates {
    val states: List<RegionInfo> by lazy { JhuRegionData.usStates.map { it.toRegionInfo() } }
    val counties: List<RegionInfo> by lazy { JhuRegionData.usCounties.map { it.toRegionInfo() } }
    val stateNames: List<String> by lazy { states.map { it.name.removeSuffix(", US") } }
    val countyNames: List<String> by lazy { counties.map { it.name.removeSuffix((", US")) } }
}