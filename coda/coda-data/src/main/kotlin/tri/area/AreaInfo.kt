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

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Key information associated with a geographic area or region. The design of this library assumes there is a unique ID
 * for every area so these can be identified uniquely, so the id is expected to be a comma-delimited set of identifiers
 * that together make it uniquely defined.
 */
open class AreaInfo(val id: String, val type: AreaType, @JsonIgnore val parent: AreaInfo?, val fips: Int? = null, val metrics: AreaMetrics) {
    init {
        require(if (parent == null) type.parents.isEmpty() else parent.type in type.parents) { "Parent type of $id was invalid: $type cannot have parent $parent" }
    }

    @get:JsonIgnore
    val population
        get() = metrics.population
    @get:JsonIgnore
    val populationPer100k
        get() = metrics.population?.let { it/100000.0 }

    override fun toString(): String {
        return "AreaInfo(id='$id', type=$type)"
    }
}

//region UNIQUE AREAS

val EARTH = AreaInfo("Earth", AreaType.PLANET, null, null, AreaMetrics(7775510000L))
//val NORTH_AMERICA = AreaInfo("North America", RegionType.CONTINENT, EARTH, null, TODO())
// USA population is sum of state and territory populations
val USA = AreaInfo("United States", AreaType.COUNTRY_REGION, EARTH, null, AreaMetrics(331808409L))
val UNKNOWN = AreaInfo("Unknown", AreaType.UNKNOWN, EARTH, null, AreaMetrics(0L))

//endregion

/** Area type. */
enum class AreaType(vararg parentTypes: AreaType, val areasInUsa: () -> List<AreaInfo>) {
    PLANET(areasInUsa = { listOf<AreaInfo>() }),
    CONTINENT(PLANET, areasInUsa = { listOf<AreaInfo>() }),
    COUNTRY_REGION(PLANET, CONTINENT, areasInUsa = { listOf<AreaInfo>(USA) }),
    PROVINCE_STATE_AGGREGATE(COUNTRY_REGION, areasInUsa = { Usa.femaRegionAreas + Usa.censusRegionAreas }),
    PROVINCE_STATE(PROVINCE_STATE_AGGREGATE, COUNTRY_REGION, areasInUsa = { Usa.stateAreas }),
    METRO(PROVINCE_STATE, COUNTRY_REGION, areasInUsa = { Usa.cbsaAreas }),
    // TODO - Alaska FIPS split, to fix when census updates
    COUNTY(METRO, PROVINCE_STATE, areasInUsa = { Usa.countyAreas.filter { it.fips !in listOf(2063, 2066) }.sortedBy { it.fips } }),
    ZIPCODE(COUNTY, METRO, PROVINCE_STATE, areasInUsa = { listOf<AreaInfo>() }),
    UNKNOWN(UNKNOWN, PROVINCE_STATE, COUNTRY_REGION, CONTINENT, PLANET, areasInUsa = { listOf<AreaInfo>() });

    val parents = listOf(*parentTypes)

    /** Get list of areas of this type in the USA, if any. */
    val areasInUs
        get() = areasInUsa()
}
