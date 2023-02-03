/*-
 * #%L
 * coda-area-usa-0.5.3-SNAPSHOT
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
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
package tri.area.usa

import org.junit.Test

class CbsaFinderTest {

    @Test
    fun testCbsaFinder() {
        println(Usa.cbsas.values.filter { it.cbsaTitle.contains(", TX") }.joinToString(", ") { it.cbsaTitle.removeSuffix(", TX") })
        println(Usa.cbsas.values.filter { it.cbsaTitle.contains(", TX") }.size)

        println(Usa.counties.values.filter { it.parent == Usa.state("TX") }.size)
        println(Usa.counties.values.filter { it.parent == Usa.state("TX") }.take(123).joinToString(", ") { it.id.removeSuffix(", Texas, US") })
        println(Usa.counties.values.filter { it.parent == Usa.state("TX") }.take(82).joinToString(", ") { it.id.removeSuffix(", Texas, US") })

        println(UsaAreaLookup.area("District of Columbia, District of Columbia, US"))
    }

}
