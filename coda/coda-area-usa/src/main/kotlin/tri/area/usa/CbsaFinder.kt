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

import tri.util.javaTrim

/** Utility to lookup CBSA's by name. */
object CbsaFinder {
    @JvmStatic
    fun main(vararg args: String) {
        println("CBSA Finder")
        println("Purpose: lookup CBSAs by exact or partial names")
        println("Usage: ./cbsa-finder.sh cbsa1,cbsa2,\"cbsa 3\"")
        println("-".repeat(20))

        val inputs = args.flatMap { it.split(",") }.map { it.javaTrim() }.toSortedSet()
        val cbsas = match(inputs)
        println("CBSA,States,CBSA Code")
        println(cbsas.map { it.cbsaTitle.replace(", ", ",") + "," + it.cbsaCode }.joinToString("\n"))
    }

    fun match(inputs: Collection<String>) = Usa.cbsas.values.filter {
        inputs.any { shortCbsa -> it.cbsaTitle caseInsensitiveContains shortCbsa || it.cbsaCode.toString() == shortCbsa }
    }
}

private infix fun String.caseInsensitiveContains(other: String) = toLowerCase().contains(other.toLowerCase())
