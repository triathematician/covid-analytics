package tri.area

/** Uniform lookup for region info by id. */
object AreaLookup {

    private val areaCache = mutableMapOf<String, AreaInfo>()
    private val notFound = mutableMapOf<String, AreaInfo>()

    init {
        areaCache["US"] = US_AREA.toAreaInfo()
    }

    /**
     * Performs lookup on given id.
     * @param id region id
     */
    operator fun invoke(id: String): AreaInfo {
        areaCache[id]?.let { return it }
        notFound[id]?.let { return it }

        val useId = when {
            id in UnitedStates.stateAbbreviations -> UnitedStates.stateFromAbbreviation(id) + ", US"
            id.removeSuffix(", US") in UnitedStates.stateAbbreviations -> UnitedStates.stateFromAbbreviation(id.removeSuffix(", US")) + ", US"
            id.toLowerCase() == "alexandria city, virginia, us" -> "Alexandria, Virginia, US"
            id.toLowerCase() == "chesapeake city, virginia, us" -> "Chesapeake, Virginia, US"
            id.toLowerCase() == "hampton city, virginia, us" -> "Hampton, Virginia, US"
            id.toLowerCase() == "manassas city, virginia, us" -> "Manassas, Virginia, US"
            id.toLowerCase() == "radford city, virginia, us" -> "Radford, Virginia, US"
            id.toLowerCase() == "lynchburg city, virginia, us" -> "Lynchburg, Virginia, US"
            id.toLowerCase() == "norfolk city, virginia, us" -> "Norfolk, Virginia, US"
            id.toLowerCase() == "portsmouth city, virginia, us" -> "Portsmouth, Virginia, US"
            id.toLowerCase() == "suffolk city, virginia, us" -> "Suffolk, Virginia, US"
            id.toLowerCase() == "virginia beach city, virginia, us" -> "Virginia Beach, Virginia, US"
            id.toLowerCase() == "newport news city, virginia, us" -> "Newport News, Virginia, US"
            id.toLowerCase() == "puerto rico, puerto rico, us" -> "Puerto Rico, US"
            id == "Fairbanks North Star Borough, Alaska, US" -> "Fairbanks North Star, Alaska, US"
            else -> id
        }

        areaCache[useId]?.let { return it }
        notFound[useId]?.let { return it }

        val region = JhuAreaData.lookupCaseInsensitive(useId)
        return if (region == null) {
            println("Area not found: $useId")
            AreaInfo(useId, RegionType.UNKNOWN, "Unknown").also { notFound[useId] = it }
        } else {
            region.toAreaInfo().also { areaCache[useId] = it }
        }
    }

    /** Lookup region by FIPS code. */
    fun fips(fips: Int): AreaInfo? = UnitedStates.fipsToCounty(fips) ?: UnitedStates.countyFipsToCbsaRegion(fips)

}