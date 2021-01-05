/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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
