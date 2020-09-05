package tri.area

import tri.util.csvKeyValues

private const val PVI = "resources/538-partisan-lean.csv"

/** Access to various regional demographic information. */
object Demographics {
    /** Get partisan voting index from 538 data. */
    val statePvi538: Map<AreaInfo, Int> by lazy {
        Demographics::class.java.getResource(PVI).csvKeyValues().map { UnitedStates.state(it["state"]!!) to it["pvi_538"]!!.pviToInt() }.toMap()
    }
}

private fun String.pviToInt() = when {
    startsWith("R+") -> -substringAfter("+").toInt()
    startsWith("D+") -> substringAfter("+").toInt()
    else -> 0
}
