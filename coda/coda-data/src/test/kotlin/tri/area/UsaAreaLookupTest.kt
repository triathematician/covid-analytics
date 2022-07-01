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
package tri.area

import junit.framework.Assert.assertEquals
import org.junit.Test

class UsaAreaLookupTest {

    @Test
    fun testLookups() {
        assertEquals(AreaType.UNKNOWN, UsaAreaLookup.area("Marshall Islands").type)
        assertEquals(AreaType.PROVINCE_STATE, UsaAreaLookup.area("Marshall Islands, US").type)
        assertEquals(AreaType.UNKNOWN, UsaAreaLookup.area("Palau").type)
        assertEquals(AreaType.PROVINCE_STATE, UsaAreaLookup.area("Palau, US").type)

        assertEquals(AreaType.COUNTY, UsaAreaLookup.area("Doña Ana, New Mexico, US").type)
        assertEquals(AreaType.UNKNOWN, UsaAreaLookup.area("Do¦a Ana, New Mexico, US").type)
        assertEquals(AreaType.COUNTY, UsaAreaLookup.area("Bristol Bay plus Lake Peninsula, Alaska, US").type)
        assertEquals(AreaType.UNKNOWN, UsaAreaLookup.area("Los Angeles, CA, US").type)
        assertEquals(AreaType.COUNTY, UsaAreaLookup.area("Los Angeles, California, US").type)
        assertEquals(AreaType.METRO, (UsaAreaLookup.area("Los Angeles, California, US") as UsCountyInfo).cbsa!!.type)

        assertEquals(AreaType.COUNTY, UsaAreaLookup.area("St. Louis City, Missouri, US").type)
        assertEquals(AreaType.UNKNOWN, UsaAreaLookup.area("Virginia Beach city, Virginia, US").type)
        assertEquals(AreaType.COUNTY, UsaAreaLookup.area("Virginia Beach, Virginia, US").type)
    }

    @Test
    fun testPopulations() {
        println(UsaAreaLookup.population("Bristol Bay plus Lake Peninsula, Alaska, US"))
        println(UsaAreaLookup.population("Los Angeles, CA, US"))

        println(UsaAreaLookup.population("New York City, New York, US"))
        println(UsaAreaLookup.population("New York-Newark-Jersey City, NY-NJ-PA, US"))
        println(UsaAreaLookup.population("New York, US"))

        println(UsaAreaLookup.population("New York, New York, US"))
        println(UsaAreaLookup.areaByFips(36061))

        println("\n--")
        println(UsaAreaLookup.area("New York-Newark-Jersey City, NY-NJ-PA").population)
        UsaAreaLookup.cbsa("New York-Newark-Jersey City, NY-NJ-PA").let {
            it.counties.forEach {
                println("${it.fips}, ${it.id}, ${it.population}")
            }
        }
    }
}
