package triathematician.covid19

import triathematician.util.CsvLineSplitter
import triathematician.util.logFirstLine
import triathematician.util.toLocalDate
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

//
// This file loads the daily time series files into memory and deals with any formatting issues.
//

private val FORMAT1 = DateTimeFormatter.ofPattern("M/d/yy H:mm")
private val FORMAT2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/** Manages access to github files */
object CovidDailyReports {
    val dir = File("c:\\code\\COVID-19\\csse_covid_19_data\\csse_covid_19_daily_reports\\")
    val allFiles
        get() = dir.walk().filter { it.extension == "csv" }.toList()

    // ﻿Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered
    val files1
        get() = dir.walk().filter { it.extension == "csv" && it.name < "03-01-2020.csv" }.toList()
    // Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered,Latitude,Longitude
    val files2
        get() = dir.walk().filter { it.extension == "csv" && it.name >= "03-01-2020.csv" && it.name <= "03-21-2020.csv" }.toList()
    // ﻿FIPS,Admin2,Province_State,Country_Region,Last_Update,Lat,Long_,Confirmed,Deaths,Recovered,Active,Combined_Key
    val files3
        get() = dir.walk().filter { it.extension == "csv" && it.name > "03-21-2020.csv" }.toList()


    /** Create time series from files. */
    fun timeSeriesData3(): Collection<MetricTimeSeries> {
        // get raw entries
        val rows = mutableListOf<DailyReportRow>()
        files3.forEach { f ->
            f.useLines { seq ->
                val fileRows = seq.drop(1).map { read3(it) }.toList()
                rows.addAll(fileRows.withAggregatedCountsForStatesAndCountries())
            }
        }

        // convert rows to time series
        val allTimeSeriesEntries = rows.flatMap {
            listOf(
                    MetricTimeSeries(it.Combined_Key, "Confirmed", it.Last_Update, it.Confirmed),
                    MetricTimeSeries(it.Combined_Key, "Deaths", it.Last_Update, it.Deaths),
                    MetricTimeSeries(it.Combined_Key, "Recovered", it.Last_Update, it.Recovered),
                    MetricTimeSeries(it.Combined_Key, "Active", it.Last_Update, it.Active)
            )
        }

        // create time series info by merging records
        return allTimeSeriesEntries.groupBy { listOf(it.id, it.metric) }
                .mapValues { it.value.merge().apply { coerceIncreasing() } }
                .values
    }

    private fun read3(line: String): DailyReportRow {
        val fields = CsvLineSplitter.splitLine(line)
        return DailyReportRow(fields[0].toIntOrNull(), fields[1], fields[2], fields[3], fields[4].toLocalDate(FORMAT1, FORMAT2),
                fields[5].toDoubleOrNull(), fields[6].toDoubleOrNull(), fields[7].toInt(), fields[8].toInt(), fields[9].toInt(),
                fields[10].toInt(), fields[11])
    }

    /** Test function to print headers from each file. */
    private fun printHeaderLines() {
        files1.forEach { it.logFirstLine() }
        println("--")
        files2.forEach { it.logFirstLine() }
        println("--")
        files3.forEach { it.logFirstLine() }
    }
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
fun List<DailyReportRow>.withAggregatedCountsForStatesAndCountries(): List<DailyReportRow> {
    val stateAggregates = filter { it.isWithinStateData }.groupBy { it.Province_State + "__" + it.Country_Region }
            .mapValues { it.value.sumWithinState() }.values
    val countryAggregates = filter { it.isWithinCountryData }.groupBy { it.Country_Region }
            .mapValues { it.value.sumWithinCountry() }.values
    return this + stateAggregates + countryAggregates
}

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinState() = DailyReportRow(null, "", first().Province_State, first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, "${first().Province_State}, ${first().Country_Region}")

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinCountry()
        = DailyReportRow(null, "", "", first().Country_Region, first().Last_Update, null, null,
        sumBy { it.Confirmed }, sumBy { it.Deaths }, sumBy { it.Recovered }, sumBy { it.Active }, first().Country_Region)