package triathematician.covid19

import triathematician.covid19.sources.CsseCovid19DailyReports
import triathematician.covid19.sources.scaledByPopulation

//
// This file links to various data sources providing time series information.
//

const val DEATHS = "Deaths"
const val DEATHS_PER_100K = "Deaths (per 100k)"
const val CASES = "Confirmed"
const val CASES_PER_100K = "Confirmed (per 100k)"
const val RECOVERED = "Recovered"
const val ACTIVE = "Active"

internal val US_STATE_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 1 }
internal val US_COUNTY_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 2 }
internal val COUNTRY_ID_FILTER: (String) -> Boolean = { !US_STATE_ID_FILTER(it) && !US_COUNTY_ID_FILTER(it) }

fun usCountyData() = dailyReports(US_COUNTY_ID_FILTER).map { it.copy(id = it.id.removeSuffix(", US")) }.sortedBy { it.id }
fun usStateData() = dailyReports(US_STATE_ID_FILTER).map { it.copy(id = it.id.removeSuffix(", US")) }.sortedBy { it.id }
fun countryData() = dailyReports(COUNTRY_ID_FILTER).sortedBy { it.id }

fun dailyReports(idFilter: (String) -> Boolean = { true }) = CsseCovid19DailyReports.allTimeSeries
        .filter { idFilter(it.id) }
        .flatMap {
            listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" }, it.movingAverage(4).growthPercentages { "$it (growth) " }
            ) + it.movingAverage(4).logisticPredictions(10)
        }