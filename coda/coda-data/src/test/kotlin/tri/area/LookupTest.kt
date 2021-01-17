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

import org.junit.Test

class LookupTest {

    @Test
    fun testLookups() {
        println(Lookup.area("Doña Ana, New Mexico, US"))
        println(Lookup.area("Do¦a Ana, New Mexico, US"))
        println(Lookup.area("Bristol Bay plus Lake Peninsula, Alaska, US"))
        println(Lookup.area("Los Angeles, CA, US"))
        println(Lookup.area("Los Angeles, California, US"))
        println((Lookup.area("Los Angeles, California, US") as UsCountyInfo).cbsa)
        println(Lookup.area("St. Louis City, Missouri, US"))
        println(Lookup.area("Virginia Beach city, Virginia, US"))
        println(Lookup.area("Virginia Beach, Virginia, US"))
    }

    @Test
    fun testPopulations() {
        println(Lookup.population("Bristol Bay plus Lake Peninsula, Alaska, US"))
        println(Lookup.population("Los Angeles, CA, US"))

        println(Lookup.population("New York City, New York, US"))
        println(Lookup.population("New York-Newark-Jersey City, NY-NJ-PA, US"))
        println(Lookup.population("New York, US"))

        println(Lookup.population("New York, New York, US"))
        println(Lookup.areaByFips(36061))

        println("\n--")
        println(Lookup.area("New York-Newark-Jersey City, NY-NJ-PA").population)
        Lookup.cbsa("New York-Newark-Jersey City, NY-NJ-PA").let {
            it.counties.forEach {
                println("${it.fips}, ${it.id}, ${it.population}")
            }
        }
    }
}
