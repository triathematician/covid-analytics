package tri.covid19

import tri.timeseries.TimeSeries

const val DEATHS = "deaths"
const val CASES = "cases"
const val RECOVERED = "recovered"
const val ACTIVE = "active"
const val BEDS = "beds"
const val VENTILATORS = "vents"
const val ICU = "icu"
const val TESTS = "tests"
const val ADMITS = "admits"

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