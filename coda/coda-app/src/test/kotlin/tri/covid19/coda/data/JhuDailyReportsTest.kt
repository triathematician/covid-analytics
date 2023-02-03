/*-
 * #%L
 * coda-app
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
package tri.covid19.coda.data

import org.junit.Test
import tri.area.usa.UsaAreaLookup
import tri.covid19.data.JhuDailyReports

class JhuDailyReportsTest {

    @Test
    fun testData() {
        println(JhuDailyReports.rawSources())
        println(JhuDailyReports.processed())
        println(JhuDailyReports.data().size)
        println(JhuDailyReports.data().size)
    }

    @Test
    fun testStates() {
        println(CovidTimeSeriesSources.dailyReports(UsaAreaLookup.area("MD"), "cases").joinToString("\n"))
    }

    @Test
    fun testCounties() {
        println(CovidTimeSeriesSources.dailyReports(UsaAreaLookup.area("Montgomery, Maryland, US"), "cases").joinToString("\n"))
    }

    @Test
    fun testCbsas() {
        println(CovidTimeSeriesSources.dailyReports(UsaAreaLookup.area("Savannah, GA, US"), "cases").joinToString("\n"))
    }

    @Test
    fun testCountries() {
        println(CovidTimeSeriesSources.dailyReports(UsaAreaLookup.area("United Kingdom"), "cases").joinToString("\n"))
    }

}

object RebuildJhuDailyReportsData {
    @JvmStatic
    fun main(args: Array<String>) {
        JhuDailyReports.reloadRawData()
    }
}
