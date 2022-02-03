/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
