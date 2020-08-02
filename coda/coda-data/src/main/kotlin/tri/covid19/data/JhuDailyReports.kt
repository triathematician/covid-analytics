package tri.covid19.data

import tri.covid19.*
import tri.regions.CbsaInfo
import tri.regions.RegionLookup
import tri.regions.UnitedStates
import tri.timeseries.MetricTimeSeries
import tri.timeseries.RegionInfo
import tri.timeseries.intTimeSeries
import tri.util.csvKeyValues
import tri.util.toLocalDate
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FORMAT1 = DateTimeFormatter.ofPattern("M/d/yy H:mm")
private val FORMAT2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val FORMAT3 = DateTimeFormatter.ofPattern("M/d/yyyy H:mm")
private val FORMAT4 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

val M_D_YYYY = DateTimeFormatter.ofPattern("M-d-yyyy")

private val FORMATS = arrayOf(FORMAT1, FORMAT2, FORMAT3, FORMAT4)

/** Processes daily time series reports into a unified time series structure. */
object JhuDailyReports: CovidDataNormalizer() {

    override fun sources() = historicalData { it.extension == "csv" }

    override fun readSource(url: URL): List<MetricTimeSeries> {
        val name = url.path.substringAfterLast("/")
        val date = name.substringBeforeLast(".").toLocalDate(M_D_YYYY)

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

        return rows.flatMap { row ->
            val region = row.region
            listOfNotNull(intTimeSeries(region, CASES, row.Last_Update, row.Confirmed),
                    intTimeSeries(region, DEATHS, row.Last_Update, row.Deaths),
                    intTimeSeries(region, RECOVERED, row.Last_Update, row.Recovered),
                    intTimeSeries(region, ACTIVE, row.Last_Update, row.Active),
                    row.People_Tested?.let { intTimeSeries(region, TESTS, row.Last_Update, it) },
                    row.People_Hospitalized?.let { intTimeSeries(region, ADMITS, row.Last_Update, it) }
            )
        }
    }

    override fun processTimeSeries(data: List<MetricTimeSeries>, coerceIncreasing: Boolean) = super.processTimeSeries(data, true)

    // region LOADING FILES INTO COMMON FORMAT

    // ﻿Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered
    private fun Map<String, String>.read1() = DailyReportRow("", "",
            get("Province/State")?.stateFix() ?: throw readError(), get("Country/Region")?.regionFix() ?: throw readError(),
            gdate("Last Update"), null, null,
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), 0, 0, 0)

    private fun Map<String, String>.readError() : IllegalStateException {
        return IllegalStateException("Unexpected fields in $this")
    }

    // Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered,Latitude,Longitude
    private fun Map<String, String>.read2() = DailyReportRow("", "",
            this["Province/State"]!!.stateFix(), this["Country/Region"]!!.regionFix(),
            gdate("Last Update"), this["Latitude"]!!.toDoubleOrNull(), this["Longitude"]!!.toDoubleOrNull(),
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), 0, 0, 0)

    // ﻿FIPS,Admin2,Province_State,Country_Region,Last_Update,Lat,Long_,Confirmed,Deaths,Recovered,Active,Combined_Key
    private fun Map<String, String>.read3() = DailyReportRow(this["FIPS"]!!, this["Admin2"]!!.adminFix(),
            this["Province_State"]!!.stateFix(), this["Country_Region"]!!.regionFix(),
            gdate("Last_Update"), this["Lat"]!!.toDoubleOrNull(), this["Long_"]!!.toDoubleOrNull(),
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), gint("Active"), gintn("People_Tested"), gintn("People_Hospitalized"))

    private fun Map<String, String>.gdate(f: String) = this[f]!!.toLocalDate(*FORMATS)
    private fun Map<String, String>.gint(f: String) = this[f]?.toIntOrNull() ?: 0
    private fun Map<String, String>.gintn(f: String) = this[f]?.toIntOrNull()

    //endregion

}

/** Daily report row info. */
data class DailyReportRow(var FIPS: String, var Admin2: String, var Province_State: String, var Country_Region: String,
                          var Last_Update: LocalDate, var Lat: Double?, var Long_: Double?,
                          var Confirmed: Int, var Deaths: Int, var Recovered: Int, var Active: Int,
                          var People_Tested: Int?, var People_Hospitalized: Int?) {

    val region: RegionInfo
        get() = RegionLookup(Combined_Key)
    val Combined_Key
        get() = listOf(Admin2, Province_State, Country_Region).filter { it.isNotEmpty() }.joinToString(", ")

    /** Data that can be aggregated at a state level. */
    val isWithinStateData
        get() = Admin2.isNotBlank() && Province_State.isNotBlank()

    /** Data that can be aggregated at a country level. */
    val isWithinCountryData
        get() = Country_Region.isNotBlank() && (Admin2.isNotBlank() || Province_State.isNotBlank())

    //region CLEANUP

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

    //endregion

}

//region CLEANUP

private val COUNTRIES_INCORRECTLY_LISTED_AS_REGIONS = listOf("United Kingdom", "Netherlands", "France", "Denmark")

/** String replacements for admin fields. */
private fun String.adminFix() = when (this) {
    "Washington County" -> "Washington"
    "Garfield County" -> "Garfield"
    "Elko County" -> "Elko"
    "Dona Ana" -> "Doña Ana"
    "LeSeur" -> "Le Sueur"
    "District of Columbia" -> ""
    "Southwest" -> "Southwest Utah"
    "unassigned" -> "Unassigned"
    else -> this
}

/** State fixes when old id/naming scheme includes state abbreviation instead of full name. */
private fun String.stateFix() = when {
    this == "Falkland Islands (Islas Malvinas)" -> "Falkland Islands (Malvinas)"
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

private fun String.regionFix() = when {
    this == "Mainland China" -> "China"
    else -> this
}

//endregion

//region aggregation across sub-regions

private val COUNTRIES_TO_NOT_AGGREGATE = listOf("United Kingdom", "Netherlands", "France", "Denmark")

/** Add state and country aggregate information to the rows. */
fun List<DailyReportRow>.withAggregations(): List<DailyReportRow> {
    val cbsaAggregates = filter { it.isWithinStateData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { UnitedStates.countyFipsToCbsa(it.FIPS.toIntOrNull() ?: 0) ?: CbsaInfo(-1, -1, "", "", "", emptyList()) }
            .filter { it.key.cbsaCode > 0 }
            .mapValues { it.value.sumWithinCbsa(it.key) }.values
    val stateAggregates = filter { it.isWithinStateData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { it.Province_State + "__" + it.Country_Region }
            .mapValues { it.value.sumWithinState() }.values
    val countryAggregates = filter { it.isWithinCountryData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { it.Country_Region }
            .mapValues { it.value.sumWithinCountry() }.values
    return this + cbsaAggregates + stateAggregates + countryAggregates + global()
}

/** Sums all counts within CBSA. */
private fun List<DailyReportRow>.sumWithinCbsa(region: CbsaInfo) = DailyReportRow("", "", region.cbsaTitle, first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, sumBy { it.People_Tested ?: 0 }, sumBy { it.People_Hospitalized ?: 0 })

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinState() = DailyReportRow("", "", first().Province_State, first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, sumBy { it.People_Tested ?: 0 }, sumBy { it.People_Hospitalized ?: 0 })

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinCountry() = DailyReportRow("", "", "", first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, sumBy { it.People_Tested ?: 0 }, sumBy { it.People_Hospitalized ?: 0 })

/** Sum of all data as a world row. */
private fun List<DailyReportRow>.global() = DailyReportRow("", "", "", "Global", first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, sumBy { it.People_Tested ?: 0 }, sumBy { it.People_Hospitalized ?: 0 })

//endregion