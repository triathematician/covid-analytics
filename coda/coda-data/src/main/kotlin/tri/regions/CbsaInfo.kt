package tri.regions

/** Information about a CBSA. */
data class CbsaInfo(val cbsaCode: Int, val csaCode: Int?, val cbsaTitle: String, val csaTitle: String, val state: String,
                    val counties: List<Int>, var population: Long = 0) {

    /** Abbreviation for core state. */
    val coreState: String
        get() = state.substringBefore("-")

    /** Region of core state. */
    val coreRegion: Int
        get() = UnitedStates.femaRegion(coreState)
}