package tri.covid19.data

import tri.area.*
import tri.covid19.*
import tri.timeseries.MetricTimeSeries
import tri.timeseries.intTimeSeries
import tri.util.csvKeyValues
import tri.util.javaTrim
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

/** List of substrings to exclude if found anywhere in area name. */
private val EXCLUDED_AREAS = listOf("Madison, Wisconsin, US", "Ashland, Nebraska, US", "Travis, California, US",
        "Madison, WI, US", "London, ON, Canada", "Others", "New York City, New York, US", "Berkeley, California, US", "US, US",
        "occupied Palestinian territory", "Fench Guiana", "Invalid", "Wuhan Evacuee", "External territories", "Jervis Bay",
        "Diamond Princess", "Grand Princess", "Cruise Ship", "Palestine", "Calgary", "Toronto", "Montreal", "Edmonton",
        "Chicago", "Boston", "Lackland", "San Antonio", "Seattle", "Tempe", "Portland", "Norwell", "Nashua",
        "Brockton", "Soldotna", "Sterling"
)

/** Processes daily time series reports into a unified time series structure. */
object JhuDailyReports : CovidDataNormalizer() {

    override fun sources() = historicalData { it.extension == "csv" }

    override fun readSource(url: URL): List<MetricTimeSeries> {
        println("Reading $url")
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
                    .filterNot { EXCLUDED_AREAS.any { t -> t in it.areaId } }
                    .onEach { it.updateTimestampsIfAfter(date) }.toList()
                    .withAggregations()
        } catch (x: Exception) {
            println("Failed to read $url"); throw x
        }

        return rows.flatMap { row ->
            val areaId = row.areaId
            val area = Lookup.areaOrNull(areaId)!!
            listOfNotNull(intTimeSeries(areaId, CASES, row.Last_Update, row.Confirmed),
                    intTimeSeries(areaId, DEATHS, row.Last_Update, row.Deaths)
//                    intTimeSeries(areaId, RECOVERED, row.Last_Update, row.Recovered)
//                    intTimeSeries(areaId, ACTIVE, row.Last_Update, row.Active),
//                    row.People_Tested?.let { intTimeSeries(areaId, TESTS, row.Last_Update, it) },
//                    row.People_Hospitalized?.let { intTimeSeries(areaId, ADMITS, row.Last_Update, it) }
            )
        }
    }

    override fun processTimeSeries(data: List<MetricTimeSeries>, coerceIncreasing: Boolean) = super.processTimeSeries(data, true)

    // region LOADING FILES INTO COMMON FORMAT

    // ﻿Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered
    private fun Map<String, String>.read1() = DailyReportRow("",
            get("Province/State")?.substringBefore(",", "")?.adminFix() ?: "",
            get("Province/State")?.substringAfter(", ")?.stateFix() ?: throw readError(),
            get("Country/Region")?.countryFix() ?: throw readError(),
            gdate("Last Update"), gint("Confirmed"), gint("Deaths"), gint("Recovered"),
            0, 0, 0)

    // Province/State,Country/Region,Last Update,Confirmed,Deaths,Recovered,Latitude,Longitude
    private fun Map<String, String>.read2() = DailyReportRow("",
            get("Province/State")?.removeSuffix(", U.S.")?.substringBefore(",", "")?.adminFix() ?: "",
            get("Province/State")?.removeSuffix(", U.S.")?.substringAfter(", ")?.javaTrim()?.stateFix() ?: throw readError(),
            this["Country/Region"]!!.countryFix(), gdate("Last Update"),
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), 0, 0, 0)

    // ﻿FIPS,Admin2,Province_State,Country_Region,Last_Update,Lat,Long_,Confirmed,Deaths,Recovered,Active,Combined_Key
    private fun Map<String, String>.read3() = DailyReportRow(this["FIPS"]!!, this["Admin2"]!!.adminFix(),
            this["Province_State"]!!, this["Country_Region"]!!, gdate("Last_Update"),
            gint("Confirmed"), gint("Deaths"), gint("Recovered"), gint("Active"), gintn("People_Tested"), gintn("People_Hospitalized"))

    private fun String.adminFix() = when {
        this in listOf("Unassigned Location", "Unknown Location", "Unknown", "Out-of-state", "unassigned") -> "Unassigned"
        startsWith("Out of") -> "Unassigned"
        else -> removeSuffix(" County").removeSuffix(" Parish").javaTrim()
    }

    private fun String.stateFix() = when (this) {
        "Virgin Islands, U.S." -> "Virgin Islands"
        in Usa.stateAbbreviations -> Usa.statesByAbbreviation[this]!!
        in listOf("None") -> ""
        else -> this
    }

    private fun String.countryFix() = when (this) {
        "Mainland China" -> "China"
        "Jersey" -> "Invalid"
        "Guernsey" -> "Invalid"
        else -> this
    }

    private fun Map<String, String>.readError(): IllegalStateException {
        return IllegalStateException("Unexpected fields in $this")
    }

    private fun Map<String, String>.gdate(f: String) = this[f]!!.toLocalDate(*FORMATS)
    private fun Map<String, String>.gint(f: String) = this[f]?.toIntOrNull() ?: 0
    private fun Map<String, String>.gintn(f: String) = this[f]?.toIntOrNull()

    //endregion

}

/** Daily report row info. */
data class DailyReportRow(var FIPS: String, var Admin2: String, var Province_State: String, var Country_Region: String,
                          var Last_Update: LocalDate,
                          var Confirmed: Int, var Deaths: Int, var Recovered: Int, var Active: Int,
                          var People_Tested: Int?, var People_Hospitalized: Int?) {

    /** Construct as aggregate, with metrics summed from other rows. */
    constructor(FIPS: String, Admin2: String, Province_State: String, Country_Region: String, data: List<DailyReportRow>)
            : this(FIPS, Admin2, Province_State, Country_Region, data.first().Last_Update,
            data.sumBy { it.Confirmed }, data.sumBy { it.Deaths }, data.sumBy { it.Recovered },
            data.sumBy { it.Active }, data.sumBy { it.People_Tested ?: 0 }, data.sumBy { it.People_Hospitalized ?: 0 })

    val areaId = listOf(Admin2, Province_State, Country_Region).filter { it.isNotEmpty() }.joinToString(", ")

    /** Data that can be aggregated at a state level. */
    val isWithinStateData = Admin2.isNotBlank() && Province_State.isNotBlank()

    /** Data that can be aggregated at a country level. */
    val isWithinCountryData = Country_Region.isNotBlank() && (Admin2.isNotBlank() || Province_State.isNotBlank())

    //region CLEANUP

    /** If the timestamp on the row is after the given name, update the date to match the file name. (If before, leave as is.) */
    internal fun updateTimestampsIfAfter(fileDate: LocalDate) {
        if (fileDate.isBefore(Last_Update)) {
            Last_Update = fileDate
        }
    }

    //endregion
}

//region aggregation across sub-regions

private val COUNTRIES_TO_NOT_AGGREGATE = listOf("United Kingdom", "Netherlands", "France", "Denmark")

/** Add state and country aggregate information to the rows. */
fun List<DailyReportRow>.withAggregations(): List<DailyReportRow> {
    val cbsaAggregates = filter { it.isWithinStateData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { Usa.cbsaCodeByCounty[it.FIPS.toIntOrNull() ?: 0] ?: 0 }
            .filter { Usa.cbsas[it.key] != null }
            .mapValues { it.value.sumWithinCbsa(Usa.cbsas[it.key]!!) }.values
    val stateAggregates = filter { it.isWithinStateData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { it.Province_State + "__" + it.Country_Region }
            .mapValues { it.value.sumWithinState() }.values
    val countryAggregates = filter { it.isWithinCountryData && it.Country_Region !in COUNTRIES_TO_NOT_AGGREGATE }
            .groupBy { it.Country_Region }
            .mapValues { it.value.sumWithinCountry() }.values
    return this + cbsaAggregates + stateAggregates + countryAggregates + global()
}

/** Sums all counts within CBSA. */
private fun List<DailyReportRow>.sumWithinCbsa(region: UsCbsaInfo) = DailyReportRow("", "", region.cbsaTitle, first().Country_Region, this)

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinState() = DailyReportRow("", "", first().Province_State, first().Country_Region, this)

/** Sums all counts. Expects the state/country pair to be the same for all. */
private fun List<DailyReportRow>.sumWithinCountry() = DailyReportRow("", "", "", first().Country_Region, this)

/** Sum of all data as a world row. */
private fun List<DailyReportRow>.global() = DailyReportRow("", "", "", "Earth", this)

//endregion