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

/** Tool for filtering CBSAs. */
class CbsaSelector(val primaryStateAbbrSelector: (String) -> Boolean, val popSelector: (Long) -> Boolean) {

    val cbsas by lazy {
        Usa.cbsas.values.filter { cbsaMatch(it.cbsaTitle) && popSelector(it.population!!) }
    }

    fun cbsaMatch(title: String) = title.substringAfter(", ").substringBefore(", US").split("-").any { primaryStateAbbrSelector(it.javaTrim()) }

}
