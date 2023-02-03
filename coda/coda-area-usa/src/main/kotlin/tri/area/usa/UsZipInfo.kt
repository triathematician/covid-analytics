/*-
 * #%L
 * coda-area-usa-0.4.0-SNAPSHOT
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

import tri.area.AreaInfo
import tri.area.AreaType

/** Information about a zipcode. */
class UsZipInfo(val zipcode: Int) : AreaInfo(checkZipCode(zipcode).toString(), AreaType.ZIPCODE, null, checkZipCode(zipcode), TODO()) {

    companion object {
        /** Checks that the zipcode is valid. */
        private fun checkZipCode(code: Int) = check(code, { it in 0..99999 }) { "Invalid zipcode: $it" }
    }

}
