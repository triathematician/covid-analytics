package triathematician.covid19.data

import triathematician.covid19.ACTIVE
import triathematician.covid19.CASES
import triathematician.covid19.DEATHS
import triathematician.covid19.RECOVERED
import triathematician.regions.lookupPopulation
import triathematician.timeseries.MetricTimeSeries
import triathematician.timeseries.intTimeSeries
import triathematician.timeseries.regroupAndMerge
import triathematician.util.CsvLineSplitter
import triathematician.util.toLocalDate
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

//
// This file loads the daily time series files into memory and deals with any formatting issues.
//

private const val DIR = "c:\\data\\COVID-19\\csse_covid_19_data\\csse_covid_19_daily_reports\\"

private val FORMAT1 = DateTimeFormatter.ofPattern("M/d/yy H:mm")
private val FORMAT2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val FORMAT3 = DateTimeFormatter.ofPattern("M/d/yyyy H:mm")
private val FORMAT4 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val FORMAT5 = DateTimeFormatter.ofPattern("MM-dd-yyyy")
private val FORMATS = arrayOf(FORMAT1, FORMAT2, FORMAT3, FORMAT4)

private val COUNTRIES_INCORRECTLY_LISTED_AS_REGIONS = listOf("United Kingdom", "Netherlands", "France", "Denmark")
private val COUNTRIES_TO_NOT_AGGREGATE = listOf("United Kingdom", "Netherlands", "France", "Denmark")

/** Manages access to github files */
object CsseCovid19DailyReports {
    private val dir = File(DIR)
    private val allFiles = dir.walk().filter { it.extension == "csv" }.toList()
    private val files1 = allFiles.filter { it.name < "03-01-2020.csv" }
    private val files2 = allFiles.filter { it.name >= "03-01-2020.csv" && it.name <= "03-21-2020.csv" }
    private val files3 = allFiles.filter { it.name > "03-21-2020.csv" }

    /** Gets all time series data. */
    val allTimeSeries by lazy {
        (timeSeriesData1() + timeSeriesData2() + timeSeriesData3()).toList().regroupAndMerge(true)
    }

    /** Create time series from format 1 files. */
    fun timeSeriesData1() = timeSeriesData(files1) { read1(it) }

    /** Create time series from format 2 files. */
    fun timeSeriesData2() = timeSeriesData(files2) { read2(it) }

    /** Create time series from format 3 files. */
    fun timeSeriesData3() = timeSeriesData(files3) { read3(it) }

    /** Create time series from format 3 files. */
    fun timeSeriesData(files: List<File>, lineReader: (List<String>) -> DailyReportRow): Collection<MetricTimeSeries> {
        // get raw entries
        val rows = mutableListOf<DailyReportRow>()
        files.forEach { f ->
            f.useLines { seq ->
                val fileDate = f.nameWithoutExtension.toLocalDate(FORMAT5)
                val fileRows = seq.drop(1).map { lineReader(CsvLineSplitter.splitLine(it)) }.toList()
                fileRows.forEach { it.updateTimestampsIfAfter(fileDate) }
                rows.addAll(fileRows.withAggregations())
            }
        }
        rows.forEach {
            if (it.Country_Region == it.Province_State && it.Country_Region in COUNTRIES_INCORRECTLY_LISTED_AS_REGIONS) {
                it.Province_State = ""
                it.Combined_Key = it.Country_Region
            }
        }

        // create time series info by merging records
        return rows.flatMap {
            listOf(intTimeSeries(it.Combined_Key, it.FIPS, CASES, it.Last_Update, it.Confirmed),
                    intTimeSeries(it.Combined_Key, it.FIPS, DEATHS, it.Last_Update, it.Deaths),
                    intTimeSeries(it.Combined_Key, it.FIPS, RECOVERED, it.Last_Update, it.Recovered),
                    intTimeSeries(it.Combined_Key, it.FIPS, ACTIVE, it.Last_Update, it.Active))
        }.regroupAndMerge(true)
    }

    // region LOADING FILES INTO COMMON FORMAT

    // ﻿Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered
    private fun read1(fields: List<String>) = DailyReportRow("", "", fields[0], fields[1].chinaFix(),
            fields[2].toLocalDate(*FORMATS), null, null,
            fields[3].toIntOrNull() ?: 0, fields[4].toIntOrNull() ?: 0, fields[5].toIntOrNull() ?: 0, 0,
            combinedKey1(fields[0], fields[1]))

    private fun combinedKey1(state: String, region: String): String {
        val fixState = when {
            state == "Chicago" -> "Chicago, Illinois"
            state.endsWith("IL") -> state.removeSuffix("IL") + "Illinois"
            state.endsWith("CA") -> state.removeSuffix("CA") + "California"
            state.endsWith("MA") -> state.removeSuffix("MA") + "Massachusetts"
            state.endsWith("WA") -> state.removeSuffix("WA") + "Washington"
            state.endsWith("AZ") -> state.removeSuffix("AZ") + "Arizona"
            state.endsWith("TX") -> state.removeSuffix("TX") + "Texas"
            state.endsWith("NE") -> state.removeSuffix("NE") + "Nebraska"
            state.endsWith("OR") -> state.removeSuffix("OR") + "Oregon"
            state.endsWith("WI") -> state.removeSuffix("WI") + "Wisconsin"
            else -> state
        }
        val fixRegion = region.chinaFix()
        return when {
            fixState.isEmpty() -> fixRegion
            else -> "$fixState, $fixRegion"
        }
    }

    private fun String.chinaFix() = if (this == "Mainland China") "China" else this

    // Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered,Latitude,Longitude
    private fun read2(fields: List<String>) = DailyReportRow("", "", fields[0], fields[1].chinaFix(),
            fields[2].toLocalDate(*FORMATS), fields[6].toDoubleOrNull(), fields[7].toDoubleOrNull(),
            fields[3].toInt(), fields[4].toInt(), fields[5].toInt(), 0, combinedKey2(fields[0], fields[1]))

    private fun combinedKey2(state: String, region: String) = when {
        state.isEmpty() -> region.chinaFix()
        else -> "$state, ${region.chinaFix()}"
    }

    // ﻿FIPS,Admin2,Province_State,Country_Region,Last_Update,Lat,Long_,Confirmed,Deaths,Recovered,Active,Combined_Key
    private fun read3(fields: List<String>) = DailyReportRow(fields[0], fields[1], fields[2], fields[3],
            fields[4].toLocalDate(*FORMATS), fields[5].toDoubleOrNull(), fields[6].toDoubleOrNull(),
            fields[7].toInt(), fields[8].toInt(), fields[9].toInt(), fields[10].toInt(), combinedKey3(fields[1], fields[2], fields[3]))

    private fun combinedKey3(admin: String, state: String, region: String): String {
        val fixAdmin = when (admin) {
            "Dona Ana" -> "Doña Ana"
            "LeSeur" -> "Le Sueur"
            else -> admin
        }
        return listOf(fixAdmin, state, region).filterNot { it.isEmpty() }.joinToString(", ")
    }

    //endregion
}

/** Daily report row info. */
data class DailyReportRow(var FIPS: String, var Admin2: String, var Province_State: String, var Country_Region: String,
                          var Last_Update: LocalDate, var Lat: Double?, var Long_: Double?,
                          var Confirmed: Int, var Deaths: Int, var Recovered: Int, var Active: Int,
                          var Combined_Key: String) {

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
}

//region aggregation across sub-regions

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
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, "${first().Province_State}, ${first().Country_Region}")

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinCountry() = DailyReportRow("", "", "", first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, first().Country_Region)

/** Sum of all data as a world row. */
private fun List<DailyReportRow>.global() = DailyReportRow("", "", "", "Global", first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, "Global")

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