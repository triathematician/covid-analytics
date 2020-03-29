package triathematician.covid19

import triathematician.util.CsvLineSplitter
import triathematician.util.toLocalDate
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

//
// This file loads the daily time series files into memory and deals with any formatting issues.
//

private val FORMAT1 = DateTimeFormatter.ofPattern("M/d/yy H:mm")
private val FORMAT2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val FORMAT3 = DateTimeFormatter.ofPattern("M/d/yyyy H:mm")
private val FORMAT4 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val FORMATS = arrayOf(FORMAT1, FORMAT2, FORMAT3, FORMAT4)

/** Manages access to github files */
object CovidDailyReports {
    private val dir = File("c:\\code\\COVID-19\\csse_covid_19_data\\csse_covid_19_daily_reports\\")
    private val allFiles = dir.walk().filter { it.extension == "csv" }.toList()
    private val files1 = allFiles.filter { it.name < "03-01-2020.csv" }
    private val files2 = allFiles.filter { it.name >= "03-01-2020.csv" && it.name <= "03-21-2020.csv" }
    private val files3 = allFiles.filter { it.name > "03-21-2020.csv" }

    /** Gets all time series data. */
    fun allTimeSeriesData() = (timeSeriesData1() + timeSeriesData2() + timeSeriesData3()).regroupAndMerge()

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
                val fileRows = seq.drop(1).map { lineReader(CsvLineSplitter.splitLine(it)) }.toList()
                rows.addAll(fileRows.withAggregations())
            }
        }

        // create time series info by merging records
        return rows.flatMap {
            listOf(MetricTimeSeries(it.Combined_Key, CASES, it.Last_Update, it.Confirmed),
                    MetricTimeSeries(it.Combined_Key, DEATHS, it.Last_Update, it.Deaths),
                    MetricTimeSeries(it.Combined_Key, RECOVERED, it.Last_Update, it.Recovered),
                    MetricTimeSeries(it.Combined_Key, ACTIVE, it.Last_Update, it.Active))
        }.regroupAndMerge()
    }

    // ﻿Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered
    private fun read1(fields: List<String>) = DailyReportRow(null, "", fields[0], fields[1].chinaFix(),
            fields[2].toLocalDate(*FORMATS), null, null,
            fields[3].toIntOrNull() ?: 0, fields[4].toIntOrNull() ?: 0, fields[5].toIntOrNull() ?: 0, 0,
            if (fields[0].isEmpty()) fields[1].chinaFix() else "${fields[0]}, ${fields[1].chinaFix()}")

    private fun String.chinaFix() = if (this == "Mainland China") "China" else this

    // Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered,Latitude,Longitude
    private fun read2(fields: List<String>) = DailyReportRow(null, "", fields[0], fields[1].chinaFix(),
            fields[2].toLocalDate(*FORMATS), fields[6].toDoubleOrNull(), fields[7].toDoubleOrNull(),
            fields[3].toInt(), fields[4].toInt(), fields[5].toInt(), 0,
            if (fields[0].isEmpty()) fields[1].chinaFix() else "${fields[0]}, ${fields[1].chinaFix()}")

    // ﻿FIPS,Admin2,Province_State,Country_Region,Last_Update,Lat,Long_,Confirmed,Deaths,Recovered,Active,Combined_Key
    private fun read3(fields: List<String>) = DailyReportRow(fields[0].toIntOrNull(), fields[1], fields[2], fields[3],
            fields[4].toLocalDate(*FORMATS), fields[5].toDoubleOrNull(), fields[6].toDoubleOrNull(),
            fields[7].toInt(), fields[8].toInt(), fields[9].toInt(), fields[10].toInt(),
            fields[11])
}

/** Daily report row info. */
data class DailyReportRow(var FIPS: Int?, var Admin2: String, var Province_State: String, var Country_Region: String,
                          var Last_Update: LocalDate, var Lat: Double?, var Long_: Double?,
                          var Confirmed: Int, var Deaths: Int, var Recovered: Int, var Active: Int,
                          var Combined_Key: String) {

    /** Data that can be aggregated at a state level. */
    val isWithinStateData
        get() = Admin2.isNotBlank() && Province_State.isNotBlank()
    /** Data that can be aggregated at a country level. */
    val isWithinCountryData
        get() = Country_Region.isNotBlank() && (Admin2.isNotBlank() || Province_State.isNotBlank())

}

/** Add state and country aggregate information to the rows. */
fun List<DailyReportRow>.withAggregations(): List<DailyReportRow> {
    val stateAggregates = filter { it.isWithinStateData }.groupBy { it.Province_State + "__" + it.Country_Region }
            .mapValues { it.value.sumWithinState() }.values
    val countryAggregates = filter { it.isWithinCountryData }.groupBy { it.Country_Region }
            .mapValues { it.value.sumWithinCountry() }.values
    return this + stateAggregates + countryAggregates + global()
}

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinState() = DailyReportRow(null, "", first().Province_State, first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, "${first().Province_State}, ${first().Country_Region}")

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinCountry() = DailyReportRow(null, "", "", first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, first().Country_Region)

/** Sum of all data as a world row. */
private fun List<DailyReportRow>.global() = DailyReportRow(null, "", "", "Global", first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, "Global")