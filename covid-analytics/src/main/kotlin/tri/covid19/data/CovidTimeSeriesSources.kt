package triathematician.covid19

import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.covid19.data.CovidHistory
import tri.regions.UnitedStates.stateNames
import tri.regions.lookupPopulation
import tri.timeseries.MetricTimeSeries
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

//
// This file links to various data sources providing time series information.
//

//region STRING UTILS AND CONSTANTS

val DEATHS_PER_100K = DEATHS.perCapita
val CASES_PER_100K = CASES.perCapita

internal val US_STATE_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 1 && it in stateNames }
internal val US_CBSA_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 2 && it.substringAfter(", ") !in stateNames }
internal val US_COUNTY_ID_FILTER: (String) -> Boolean = { ", US" in it && it.count { it == ',' } == 2 && it.substringAfter(", ") in stateNames }
internal val COUNTRY_ID_FILTER: (String) -> Boolean = { !US_STATE_ID_FILTER(it) && !US_COUNTY_ID_FILTER(it) && !US_CBSA_ID_FILTER(it) }

internal val String.perCapita
    get() = "$this (per 100k)"

//endregion

/** Primary access point for COVID time series data. */
@ExperimentalTime
object CovidTimeSeriesSources {

    val dailyCountryReports by lazy { dailyReports(COUNTRY_ID_FILTER) }
    val dailyUsCountyReports by lazy { dailyReports(US_COUNTY_ID_FILTER) }
    val dailyUsStateReports by lazy { dailyReports(US_STATE_ID_FILTER) }
    val dailyUsCbsaReports by lazy { dailyReports(US_CBSA_ID_FILTER) }

    /** Easy access to county data. */
    fun usCountyData() = dailyUsCountyReports
            .map { it.copy(group = it.group.removeSuffix(", US")) }
            .sortedBy { it.group }

    /** Easy access to county data. */
    fun usCbsaData() = dailyUsCbsaReports
            .map { it.copy(group = it.group.removeSuffix(", US")) }
            .sortedBy { it.group }

    /** Easy access to state data. */
    fun usStateData(includeUS: Boolean = true) = dailyUsStateReports
            .filter { includeUS || it.group != "US" }
            .map { it.copy(group = it.group.removeSuffix(", US")) }
            .sortedBy { it.group }

    /** Easy access to country data. */
    fun countryData(includeGlobal: Boolean = true) = dailyCountryReports
            .filter { includeGlobal || it.group != "Global" }
            .sortedBy { it.group }

    /** Get daily reports for given regions, with additional metrics giving daily growth rates and logistic fit predictions. */
    fun dailyReports(idFilter: (String) -> Boolean = { true }, averageDays: Int = 7) = measureTimedValue {
        CovidHistory.allData
                .filter { idFilter(it.group) }
                .flatMap {
                    listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" }, it.movingAverage(averageDays).growthPercentages { "$it (growth)" }) +
                            it.movingAverage(averageDays).shortTermLogisticForecast(10)
                }
    }.also {
        println("Loaded region group data with predictions in ${it.duration}")
    }.value

    /** Filter daily reports for selected region and metric. */
    fun dailyReports(region: String, metric: String, relatedSeries: Boolean = true) = dailyReports({ it == region })
            .filter { if (relatedSeries) metric in it.metric else metric == it.metric }
}

//region population lookups

fun MetricTimeSeries.scaledByPopulation(metricFunction: (String) -> String) = when (val pop = lookupPopulation(group)) {
    null -> null
    else -> (this / (pop.toDouble() / 100000)).also {
        it.intSeries = false
        it.metric = metricFunction(it.metric)
    }
}

//endregion