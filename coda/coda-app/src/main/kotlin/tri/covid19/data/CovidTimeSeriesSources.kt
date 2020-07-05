package tri.covid19.data

import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.timeseries.MetricTimeSeries
import tri.timeseries.RegionInfo
import tri.timeseries.RegionType
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.milliseconds

//
// This file links to various data sources providing time series information.
//

//region STRING UTILS AND CONSTANTS

val DEATHS_PER_100K = DEATHS.perCapita
val CASES_PER_100K = CASES.perCapita

internal val US_STATE_ID_FILTER: (RegionInfo) -> Boolean = { it.type == RegionType.PROVINCE_STATE && it.parent == "US" }
internal val US_CBSA_ID_FILTER: (RegionInfo) -> Boolean = { it.type == RegionType.METRO && it.parent == "US" }
internal val US_COUNTY_ID_FILTER: (RegionInfo) -> Boolean = { it.type == RegionType.COUNTY && "US" in it.parent }
internal val COUNTRY_ID_FILTER: (RegionInfo) -> Boolean = { it.type == RegionType.COUNTRY_REGION }

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
            .map { it.copy(region = it.region.copy(id = it.region.id.removeSuffix(", US"))) }
            .sortedBy { it.region.id }

    /** Easy access to county data. */
    fun usCbsaData() = dailyUsCbsaReports
            .map { it.copy(region = it.region.copy(id = it.region.id.removeSuffix(", US"))) }
            .sortedBy { it.region.id }

    /** Easy access to state data. */
    fun usStateData(includeUS: Boolean = true) = dailyUsStateReports
            .filter { includeUS || it.region.id != "US" }
            .map { it.copy(region = it.region.copy(id = it.region.id.removeSuffix(", US"))) }
            .sortedBy { it.region.id }

    /** Easy access to country data. */
    fun countryData(includeGlobal: Boolean = true) = dailyCountryReports
            .filter { includeGlobal || it.region.id != "Global" }
            .sortedBy { it.region.id }

    /** Get daily reports for given regions, with additional metrics giving daily growth rates and logistic fit predictions. */
    fun dailyReports(idFilter: (RegionInfo) -> Boolean = { true }, averageDays: Int = 7) = measureTimedValue {
        CovidHistory.allData
                .filter { idFilter(it.region) }
                .flatMap {
                    listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" }, it.movingAverage(averageDays).growthPercentages { "$it (growth)" }) +
                            it.movingAverage(averageDays).shortTermLogisticForecast(10)
                }
    }.also {
        if (it.duration > 100.milliseconds) println("Loaded region group data with predictions in ${it.duration}")
    }.value

    /** Filter daily reports for selected region and metric. */
    fun dailyReports(region: RegionInfo, metric: String, relatedSeries: Boolean = true) = dailyReports({ it == region })
            .filter { if (relatedSeries) metric in it.metric else metric == it.metric }
}

//region population lookups

fun MetricTimeSeries.scaledByPopulation(metricFunction: (String) -> String) = when (val pop = region.population) {
    null -> null
    else -> (this / (pop.toDouble() / 100000)).also {
        it.intSeries = false
        it.metric = metricFunction(it.metric)
    }
}

//endregion