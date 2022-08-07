package tri.area

/** Area type. */
enum class AreaType(vararg parentTypes: AreaType) {
    PLANET(),
    CONTINENT(PLANET),
    COUNTRY_REGION(PLANET, CONTINENT),
    PROVINCE_STATE_AGGREGATE(COUNTRY_REGION),
    PROVINCE_STATE(PROVINCE_STATE_AGGREGATE, COUNTRY_REGION),
    METRO(PROVINCE_STATE, COUNTRY_REGION),
    COUNTY(METRO, PROVINCE_STATE),
    ZIPCODE(COUNTY, METRO, PROVINCE_STATE),
    UNKNOWN(UNKNOWN, PROVINCE_STATE, COUNTRY_REGION, CONTINENT, PLANET);

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

    val parents = listOf(*parentTypes)
}
