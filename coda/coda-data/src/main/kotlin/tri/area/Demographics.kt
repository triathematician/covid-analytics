package tri.area

import tri.util.csvResource

private const val PVI = "resources/538-partisan-lean.csv"

/** Access to various regional demographic information. */
object Demographics {
    /** Get partisan voting index from 538 data. */
    val statePvi538 = Demographics::class.csvResource<PviEntry>(true, PVI)
            .map { Usa.stateByLongName(it.state) to it.pvi }.toMap()
}

private class PviEntry(val state: String, pvi_538: String) {
    val pvi = when {
        pvi_538.startsWith("R+") -> -pvi_538.substringAfter("+").toInt()
        pvi_538.startsWith("D+") -> pvi_538.substringAfter("+").toInt()
        else -> 0
    }
}
