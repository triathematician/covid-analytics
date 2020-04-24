package triathematician.covid19

import triathematician.covid19.sources.CsseCovid19DailyReports
import triathematician.covid19.sources.scaledByPopulation

//
// This file links to various data sources providing time series information.
//

//region STRING UTILS AND CONSTANTS

const val DEATHS = "Deaths"
const val CASES = "Confirmed"
const val RECOVERED = "Recovered"
const val ACTIVE = "Active"

val DEATHS_PER_100K = DEATHS.perCapita
val CASES_PER_100K = CASES.perCapita

internal val US_STATE_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 1 }
internal val US_COUNTY_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 2 }
internal val COUNTRY_ID_FILTER: (String) -> Boolean = { !US_STATE_ID_FILTER(it) && !US_COUNTY_ID_FILTER(it) }

internal val String.perCapita
    get() = "$this (per 100k)"

//endregion

/** Primary access point for COVID time series data. */
object CovidTimeSeriesSources {

    val dailyCountryReports by lazy { dailyReports(COUNTRY_ID_FILTER) }
    val dailyUsCountyReports by lazy { dailyReports(US_COUNTY_ID_FILTER) }
    val dailyUsStateReports by lazy { dailyReports(US_STATE_ID_FILTER) }

    /** Easy access to county data. */
    fun usCountyData() = dailyUsCountyReports
            .map { it.copy(id = it.id.removeSuffix(", US")) }
            .sortedBy { it.id }

    /** Easy access to state data. */
    fun usStateData(includeUS: Boolean = true) = dailyUsStateReports
            .filter { includeUS || it.id != "US" }
            .map { it.copy(id = it.id.removeSuffix(", US")) }
            .sortedBy { it.id }

    /** Easy access to country data. */
    fun countryData(includeGlobal: Boolean = true) = dailyCountryReports
            .filter { includeGlobal || it.id != "Global" }
            .sortedBy { it.id }

    /** Get daily reports for given regions, with additional metrics giving daily growth rates and logistic fit predictions. */
    fun dailyReports(idFilter: (String) -> Boolean = { true }, averageDays: Int = 7) = CsseCovid19DailyReports.allTimeSeries
            .filter { idFilter(it.id) }
            .flatMap {
                listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" }, it.movingAverage(averageDays).growthPercentages { "$it (growth)" }
                ) + it.movingAverage(averageDays).logisticPredictions(10)
            }

    /** Filter daily reports for selected region and metric. */
    fun dailyReports(region: String, metric: String, relatedSeries: Boolean = true) = dailyReports({ it == region })
            .filter { if (relatedSeries) metric in it.metric else metric == it.metric }
}