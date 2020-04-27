package tri.covid19.data

import tri.covid19.*
import tri.regions.lookupPopulation
import tri.timeseries.MetricTimeSeries
import tri.timeseries.RegionTimeSeries
import tri.timeseries.intTimeSeries
import tri.timeseries.regroupAndMerge
import tri.util.csvKeyValues
import tri.util.toLocalDate
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FORMAT1 = DateTimeFormatter.ofPattern("M/d/yy H:mm")
private val FORMAT2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val FORMAT3 = DateTimeFormatter.ofPattern("M/d/yyyy H:mm")
private val FORMAT4 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val MM_DD_YYYY = DateTimeFormatter.ofPattern("MM-dd-yyyy")
private val FORMATS = arrayOf(FORMAT1, FORMAT2, FORMAT3, FORMAT4)

/** Processes daily time series reports into a unified time series structure. */
object JhuDailyReports: CovidDataNormalizer() {

    override fun sources() = historicalData { it.extension == "csv" }

    override fun readSource(url: URL): List<MetricTimeSeries> {
        val name = url.path.substringAfterLast("/")
        val date = name.substringBeforeLast(".").toLocalDate(MM_DD_YYYY)

        val lineReader: (Map<String, String>) -> DailyReportRow = when {
            name < "03-01-2020.csv" -> { m -> m.read1() }
            name >= "03-01-2020.csv" && name <= "03-21-2020.csv" -> { m -> m.read2() }
            name > "03-21-2020.csv" -> { m -> m.read3() }
            else -> throw IllegalStateException()
        }

        val rows = try {
            url.csvKeyValues().map(lineReader)
                    .onEach { it.updateTimestampsIfAfter(date) }.toList()
                    .onEach { it.fixCountriesIncorrectlyListedAsRegions() }
                    .withAggregations()
        } catch (x: Exception) { println("Failed to read $url"); throw x }

        return rows.flatMap {
            listOf(intTimeSeries(it.Combined_Key, it.FIPS, CASES, it.Last_Update, it.Confirmed),
                    intTimeSeries(it.Combined_Key, it.FIPS, DEATHS, it.Last_Update, it.Deaths),
                    intTimeSeries(it.Combined_Key, it.FIPS, RECOVERED, it.Last_Update, it.Recovered),
                    intTimeSeries(it.Combined_Key, it.FIPS, ACTIVE, it.Last_Update, it.Active))
        }
    }

    override fun processTimeSeries(data: List<MetricTimeSeries>, coerceIncreasing: Boolean) = super.processTimeSeries(data, true)

    // region LOADING FILES INTO COMMON FORMAT

    // ﻿Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered
    private fun Map<String, String>.read1() = DailyReportRow("", "",
            this["Province/State"]!!.stateFix(), this["Country/Region"]!!.regionFix(),
            gdate("Last Update"), null, null,
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), 0)

    // Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered,Latitude,Longitude
    private fun Map<String, String>.read2() = DailyReportRow("", "",
            this["Province/State"]!!.stateFix(), this["Country/Region"]!!.regionFix(),
            gdate("Last Update"), this["Latitude"]!!.toDoubleOrNull(), this["Longitude"]!!.toDoubleOrNull(),
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), 0)

    // ﻿FIPS,Admin2,Province_State,Country_Region,Last_Update,Lat,Long_,Confirmed,Deaths,Recovered,Active,Combined_Key
    private fun Map<String, String>.read3() = DailyReportRow(this["FIPS"]!!, this["Admin2"]!!.adminFix(),
            this["Province_State"]!!, this["Country_Region"]!!.regionFix(),
            gdate("Last_Update"), this["Lat"]!!.toDoubleOrNull(), this["Long_"]!!.toDoubleOrNull(),
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), gint("Active"))

    private fun Map<String, String>.gdate(f: String) = this[f]!!.toLocalDate(*FORMATS)
    private fun Map<String, String>.gint(f: String) = this[f]?.toIntOrNull() ?: 0

    //endregion

}

/** Daily report row info. */
data class DailyReportRow(var FIPS: String, var Admin2: String, var Province_State: String, var Country_Region: String,
                          var Last_Update: LocalDate, var Lat: Double?, var Long_: Double?,
                          var Confirmed: Int, var Deaths: Int, var Recovered: Int, var Active: Int) {

    val Combined_Key
        get() = combinedKey(Admin2, Province_State, Country_Region)

    /** Data that can be aggregated at a state level. */
    val isWithinStateData
        get() = Admin2.isNotBlank() && Province_State.isNotBlank()

    /** Data that can be aggregated at a country level. */
    val isWithinCountryData
        get() = Country_Region.isNotBlank() && (Admin2.isNotBlank() || Province_State.isNotBlank())

    /** If the timestamp on the row is after the given name, update the date to match the file name. (If before, leave as is.) */
    internal fun updateTimestampsIfAfter(fileDate: LocalDate) {
        if (fileDate.isBefore(Last_Update)) {
            Last_Update = fileDate
        }
    }

    fun fixCountriesIncorrectlyListedAsRegions() {
        if (Country_Region == Province_State && Country_Region in COUNTRIES_INCORRECTLY_LISTED_AS_REGIONS) {
            Province_State = ""
        }
    }
}

//region ID CLEANUP

private val COUNTRIES_INCORRECTLY_LISTED_AS_REGIONS = listOf("United Kingdom", "Netherlands", "France", "Denmark")

private fun combinedKey(admin: String, state: String, region: String)
        = listOf(admin.adminFix(), state.stateFix(), region.regionFix()).filterNot { it.isEmpty() }.joinToString(", ")

private fun String.adminFix() = when (this) {
    "Dona Ana" -> "Doña Ana"
    "LeSeur" -> "Le Sueur"
    else -> this
}

private fun String.stateFix() = when {
    this == "Chicago" -> "Chicago, Illinois"
    endsWith("IL") -> removeSuffix("IL") + "Illinois"
    endsWith("CA") -> removeSuffix("CA") + "California"
    endsWith("MA") -> removeSuffix("MA") + "Massachusetts"
    endsWith("WA") -> removeSuffix("WA") + "Washington"
    endsWith("AZ") -> removeSuffix("AZ") + "Arizona"
    endsWith("TX") -> removeSuffix("TX") + "Texas"
    endsWith("NE") -> removeSuffix("NE") + "Nebraska"
    endsWith("OR") -> removeSuffix("OR") + "Oregon"
    endsWith("WI") -> removeSuffix("WI") + "Wisconsin"
    else -> this
}

private fun String.regionFix() = if (this == "Mainland China") "China" else this

//endregion

//region aggregation across sub-regions

private val COUNTRIES_TO_NOT_AGGREGATE = listOf("United Kingdom", "Netherlands", "France", "Denmark")

/** Add state and country aggregate information to the rows. */
fun List<DailyReportRow>.withAggregations(): List<DailyReportRow> {
    val stateAggregates = filter { it.isWithinStateData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { it.Province_State + "__" + it.Country_Region }
            .mapValues { it.value.sumWithinState() }.values
    val countryAggregates = filter { it.isWithinCountryData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { it.Country_Region }
            .mapValues { it.value.sumWithinCountry() }.values
    return this + stateAggregates + countryAggregates + global()
}

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinState() = DailyReportRow("", "", first().Province_State, first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active })

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinCountry() = DailyReportRow("", "", "", first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active })

/** Sum of all data as a world row. */
private fun List<DailyReportRow>.global() = DailyReportRow("", "", "", "Global", first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active })

//endregion

//region population lookups

fun MetricTimeSeries.scaledByPopulation(metricFunction: (String) -> String) = when (val pop = lookupPopulation(id)) {
    null -> null
    else -> (this / (pop.toDouble() / 100000)).also {
        it.intSeries = false
        it.metric = metricFunction(it.metric)
    }
}

//endregion