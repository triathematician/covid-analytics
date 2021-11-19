/*-
 * #%L
 * coda-data-0.1.21-SNAPSHOT
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

import tri.util.csvResource

/** Loads data from files with information about counties, CBSAs, FIPS, and more. */
internal object UsaSourceData {

    /** County FIPS data. */
    internal val fips = Usa::class.csvResource<CountyFips>(true, "resources/fips.csv")

    /** State/territory FIPS data, with Census Region/Division. */
    internal val stateFips = Usa::class.csvResource<StateFips>(true, "resources/state-fips.csv")

    /** CBSA/County mapping data. */
    internal val cbsaData = Usa::class.csvResource<CbsaCountyMapping>(true, "resources/census/Mar2020cbsaDelineation.csv")

    //region DATA LOADER CLASSES

    /** Mapping for state FIPS file. */
    internal class StateFips(val state_name: String, val state_abbr: String, val long_name: String,
                             val fips: Int, val sumlev: Int, val region: Int, val division: Int, val state: Int,
                             val region_name: String, val division_name: String, val fema_region: Int)

    /** Mapping for FIPS file. */
    internal class CountyFips(val fips: Int, val name: String, val state: String)

    /** Mapping for CBSA delineation file. */
    internal class CbsaCountyMapping(val CBSA_Code: Int, val Metropolitan_Division_Code: String, val CSA_Code: Int,
                                     val CBSA_Title: String, val CBSA_Type: String,
                                     val Metropolitan_Division_Title: String, val CSA_Title: String,
                                     val County_or_Equivalent: String, val State_Name: String,
                                     val FIPS_State_Code: Int, val FIPS_County_Code: Int, val FIPS_Combined: Int,
                                     val Central_Outlying_County: String) {
        val fipsCombined = FIPS_State_Code * 1000 + FIPS_County_Code
    }

    //endregion

}