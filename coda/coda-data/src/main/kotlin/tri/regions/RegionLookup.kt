package tri.regions

import tri.timeseries.RegionInfo
import tri.timeseries.RegionType

/** Uniform lookup for region info by id. */
object RegionLookup {

    private val regionCache = mutableMapOf<String, RegionInfo>()
    private val notFound = mutableMapOf<String, RegionInfo>()

    /**
     * Performs lookup on given id.
     * @param id region id
     */
    operator fun invoke(id: String): RegionInfo {
        regionCache[id]?.let { return it }
        notFound[id]?.let { return it }

        val useId = when {
            id in UnitedStates.stateAbbreviations ->
                UnitedStates.stateFromAbbreviation(id) + ", US"
            id.removeSuffix(", US") in UnitedStates.stateAbbreviations ->
                UnitedStates.stateFromAbbreviation(id.removeSuffix(", US")) + ", US"
            else -> id
        }

        regionCache[useId]?.let { return it }
        notFound[useId]?.let { return it }

        val region = JhuRegionData.data[useId]
        return if (region == null) {
            println("Region not found: $useId")
            RegionInfo(useId, RegionType.UNKNOWN, "Unknown").also { notFound[useId] = it }
        } else {
            region.toRegionInfo().also { regionCache[useId] = it }
        }
    }

}