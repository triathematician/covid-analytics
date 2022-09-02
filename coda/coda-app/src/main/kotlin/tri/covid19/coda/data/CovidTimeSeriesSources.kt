/*-
 * #%L
 * coda-app
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
package tri.covid19.coda.data

import tri.area.*
import tri.area.usa.UsaAreaLookup
import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.covid19.data.LocalCovidDataQuery
import tri.timeseries.TimeSeries
import tri.timeseries.analytics.shortTermLogisticForecast
import tri.util.measureTimedValue
import kotlin.time.Duration.Companion.milliseconds

//
// This file links to various data sources providing time series information.
//

//region STRING UTILS AND CONSTANTS

val DEATHS_PER_100K = DEATHS.perCapita
val CASES_PER_100K = CASES.perCapita

internal val US_STATE_FILTER: (AreaInfo) -> Boolean = { it.type == AreaType.PROVINCE_STATE && it.parent == USA }
internal val US_CBSA_FILTER: (AreaInfo) -> Boolean = { it.type == AreaType.METRO && it.parent == USA }
internal val US_COUNTY_FILTER: (AreaInfo) -> Boolean = { it.type == AreaType.COUNTY && it.parent?.parent == USA }
internal val COUNTRY_FILTER: (AreaInfo) -> Boolean = { it.type == AreaType.COUNTRY_REGION }

internal val String.perCapita
    get() = "$this (per 100k)"

//endregion

/** Primary access point for COVID time series data. */
object CovidTimeSeriesSources {

    val dailyCountryReports by lazy { dailyReports(COUNTRY_FILTER) }
    val dailyUsCountyReports by lazy { dailyReports(US_COUNTY_FILTER) }
    val dailyUsStateReports by lazy { dailyReports(US_STATE_FILTER) }
    val dailyUsCbsaReports by lazy { dailyReports(US_CBSA_FILTER) }

    /** Easy access to county data. */
    fun usCountyData() = dailyUsCountyReports.sortedBy { it.areaId }

    /** Easy access to county data. */
    fun usCbsaData() = dailyUsCbsaReports.sortedBy { it.areaId }

    /** Easy access to state data. */
    fun usStateData(includeUS: Boolean = true) = dailyUsStateReports
            .filter { includeUS || UsaAreaLookup.area(it.areaId) != USA }
            .sortedBy { it.areaId }

    /** Easy access to country data. */
    fun countryData(includeGlobal: Boolean = true) = dailyCountryReports
            .filter { includeGlobal || UsaAreaLookup.area(it.areaId) != EARTH }
            .sortedBy { it.areaId }

    /** Get daily reports for given regions, with additional metrics giving daily growth rates and logistic fit predictions. */
    private fun dailyReports(areaFilter: (AreaInfo) -> Boolean = { true }, averageDays: Int = 7) = measureTimedValue {
        LocalCovidDataQuery.allDataByArea(areaFilter)
                .flatMap { it.value }
                .flatMap {
                    listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" },
                            it.movingAverage(averageDays).symmetricGrowth { "$it (growth)" }) +
                            it.movingAverage(averageDays).shortTermLogisticForecast(10)
                }
    }.also {
        if (it.duration > 100.milliseconds) println("Loaded filtered area data with derived series (pop/growth/forecast) in ${it.duration}")
    }.value

    /** Filter daily reports for selected region and metric. */
    fun dailyReports(area: AreaInfo, metric: String, relatedSeries: Boolean = true) = dailyReports({ it == area })
            .filter { if (relatedSeries) metric in it.metric else metric == it.metric }
}

//region population lookups

fun TimeSeries.scaledByPopulation(metricFunction: (String) -> String) = when (val pop = UsaAreaLookup.area(areaId).population) {
    null -> null
    else -> (this / (pop.toDouble() / 100000)).also {
        it.intSeries = false
        it.metric = metricFunction(it.metric)
    }
}

//endregion
