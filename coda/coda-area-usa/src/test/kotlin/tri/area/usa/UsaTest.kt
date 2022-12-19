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
package tri.area.usa

import org.junit.Test
import kotlin.test.assertEquals

class UsaTest {

    @Test
    fun testStates() {
        assertEquals(60, Usa.stateAreas.size)

        Usa.stateByLongName("Federated States of Micronesia")
        Usa.stateByLongName("Palau")
    }

    @Test
    fun testUsaAncestors() {
        println(UsaAreaLookup.area("24027").ancestors().map { it.id })
        println(UsaAreaLookup.area("MD").ancestors().map { it.id })
        println(UsaAreaLookup.area("Region 3").ancestors().map { it.id })
        println(UsaAreaLookup.area("USA").ancestors().map { it.id })

        println(UsaAreaLookup.area("Region 1").ancestors().map { it.id })
    }
}
