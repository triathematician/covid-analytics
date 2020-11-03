package tri.covid19

import tri.timeseries.TimeSeries

const val DEATHS = "Deaths"
const val CASES = "Confirmed"
const val RECOVERED = "Recovered"
const val ACTIVE = "Active"
const val BEDS = "Beds"
const val VENTILATORS = "Ventilators"
const val ICU = "ICU"
const val TESTS = "Tests"
const val ADMITS = "Admits"

//region FILTER XFs

val List<TimeSeries>.cases
    get() = first { it.metric == CASES }
val List<TimeSeries>.deaths
    get() = first { it.metric == DEATHS }

val List<TimeSeries>.allCases
    get() = filter { it.metric == CASES }
val List<TimeSeries>.allDeaths
    get() = filter { it.metric == DEATHS }

//endregion