package triathematician.covid19

import triathematician.covid19.sources.CsseCovid19DailyReports
import triathematician.covid19.sources.scaledByPopulation

//
// This file links to various data sources providing time series information.
//

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

fun usCountyData() = CovidTimeSeriesSources.dailyUsCountyReports.map { it.copy(id = it.id.removeSuffix(", US")) }.sortedBy { it.id }
fun usStateData(includeUS: Boolean = true) = CovidTimeSeriesSources.dailyUsStateReports
        .filter { includeUS || it.id != "US" }
        .map { it.copy(id = it.id.removeSuffix(", US")) }.sortedBy { it.id }
fun countryData(includeGlobal: Boolean = true) = CovidTimeSeriesSources.dailyCountryReports
        .filter { includeGlobal || it.id != "Global" }
        .sortedBy { it.id }

object CovidTimeSeriesSources {

    val dailyCountryReports by lazy { dailyReports(COUNTRY_ID_FILTER) }
    val dailyUsCountyReports by lazy { dailyReports(US_COUNTY_ID_FILTER) }
    val dailyUsStateReports by lazy { dailyReports(US_STATE_ID_FILTER) }

    fun dailyReports(idFilter: (String) -> Boolean = { true }, bucketGrowthAverage: Int = 7) = CsseCovid19DailyReports.allTimeSeries
            .filter { idFilter(it.id) }
            .flatMap {
                listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" }, it.movingAverage(bucketGrowthAverage).growthPercentages { "$it (growth)" }
                ) + it.movingAverage(bucketGrowthAverage).logisticPredictions(10)
            }
}