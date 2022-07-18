/*-
 * #%L
 * coda-time-0.4.0-SNAPSHOT
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
package tri.area

/** Looks up an area by id. */
interface AreaLookup {
    /** Lookup an area by id or name, returning null if none. */
    fun areaOrNull(lookupName: String, assumeUsState: Boolean = false): AreaInfo?

    /**
     * Get object for area with given name. Logs an error and returns a generic "Unknown" area if not found.
     * @param lookupName name to lookup
     * @param assumeUsState if true, lookup will assume the area is part of the USA if not found or ambiguous
     */
    fun area(lookupName: String, assumeUsState: Boolean = false) = areaOrNull(lookupName, assumeUsState) ?: UNKNOWN
}


//region UNIQUE AREAS

/** Earth area. */
val EARTH = AreaInfo("Earth", AreaType.PLANET, null, null, AreaMetrics(7775510000L))
///** North American area. */
//val NORTH_AMERICA = AreaInfo("North America", RegionType.CONTINENT, EARTH, null, TODO())
/** USA area. */
val USA = AreaInfo("United States", AreaType.COUNTRY_REGION, EARTH, null, AreaMetrics(331808409L))

/** Unknown area. */
val UNKNOWN = AreaInfo("Unknown", AreaType.UNKNOWN, EARTH, null, AreaMetrics(0L))

//endregion


