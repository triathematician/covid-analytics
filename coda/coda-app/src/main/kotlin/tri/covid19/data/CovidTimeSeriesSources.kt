package tri.covid19.data

import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.timeseries.MetricTimeSeries
import tri.area.AreaInfo
import tri.area.RegionType
import tri.area.USA
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.milliseconds

//
// This file links to various data sources providing time series information.
//

//region STRING UTILS AND CONSTANTS

val DEATHS_PER_100K = DEATHS.perCapita
val CASES_PER_100K = CASES.perCapita

internal val US_STATE_FILTER: (AreaInfo) -> Boolean = { it.type == RegionType.PROVINCE_STATE && it.parent == USA }
internal val US_CBSA_FILTER: (AreaInfo) -> Boolean = { it.type == RegionType.METRO && it.parent == USA }
internal val US_COUNTY_FILTER: (AreaInfo) -> Boolean = { it.type == RegionType.COUNTY && it.parent?.parent == USA }
internal val COUNTRY_FILTER: (AreaInfo) -> Boolean = { it.type == RegionType.COUNTRY_REGION }

internal val String.perCapita
    get() = "$this (per 100k)"

//endregion

/** Primary access point for COVID time series data. */
@ExperimentalTime
object CovidTimeSeriesSources {

    val dailyCountryReports by lazy { dailyReports(COUNTRY_FILTER) }
    val dailyUsCountyReports by lazy { dailyReports(US_COUNTY_FILTER) }
    val dailyUsStateReports by lazy { dailyReports(US_STATE_FILTER) }
    val dailyUsCbsaReports by lazy { dailyReports(US_CBSA_FILTER) }

    /** Easy access to county data. */
    fun usCountyData() = dailyUsCountyReports.sortedBy { it.area.id }

    /** Easy access to county data. */
    fun usCbsaData() = dailyUsCbsaReports.sortedBy { it.area.id }

    /** Easy access to state data. */
    fun usStateData(includeUS: Boolean = true) = dailyUsStateReports
            .filter { includeUS || it.area.id != "US" }
            .sortedBy { it.area.id }

    /** Easy access to country data. */
    fun countryData(includeGlobal: Boolean = true) = dailyCountryReports
            .filter { includeGlobal || it.area.id != "Global" }
            .sortedBy { it.area.id }

    /** Get daily reports for given regions, with additional metrics giving daily growth rates and logistic fit predictions. */
    fun dailyReports(idFilter: (AreaInfo) -> Boolean = { true }, averageDays: Int = 7) = measureTimedValue {
        CovidHistory.allData
                .filter { idFilter(it.area) }
                .flatMap {
                    listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" }, it.movingAverage(averageDays).growthPercentages { "$it (growth)" }) +
                            it.movingAverage(averageDays).shortTermLogisticForecast(10)
                }
    }.also {
        if (it.duration > 100.milliseconds) println("Loaded region group data with predictions in ${it.duration}")
    }.value

    /** Filter daily reports for selected region and metric. */
    fun dailyReports(area: AreaInfo, metric: String, relatedSeries: Boolean = true) = dailyReports({ it == area })
            .filter { if (relatedSeries) metric in it.metric else metric == it.metric }
}

//region population lookups

fun MetricTimeSeries.scaledByPopulation(metricFunction: (String) -> String) = when (val pop = area.population) {
    null -> null
    else -> (this / (pop.toDouble() / 100000)).also {
        it.intSeries = false
        it.metric = metricFunction(it.metric)
    }
}

//endregion