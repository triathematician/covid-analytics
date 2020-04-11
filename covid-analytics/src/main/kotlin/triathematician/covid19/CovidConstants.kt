package triathematician.covid19

const val DEATHS = "Deaths"
const val DEATHS_PER_100K = "Deaths (per 100k)"
const val CASES = "Confirmed"
const val CASES_PER_100K = "Confirmed (per 100k)"
const val RECOVERED = "Recovered"
const val ACTIVE = "Active"

internal val US_STATE_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 1 }
internal val US_COUNTY_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 2 }
internal val COUNTRY_ID_FILTER: (String) -> Boolean = { !US_STATE_ID_FILTER(it) && !US_COUNTY_ID_FILTER(it) }