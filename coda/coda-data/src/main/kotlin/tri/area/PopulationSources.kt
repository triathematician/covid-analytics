package tri.area

import tri.util.csvLines

//
// This file provides access to population data tables.
//

private const val COUNTY_CENSUS_DATA = "resources/county census.csv"
private const val STATE_CENSUS_DATA = "resources/state census.csv"
private const val METRO_DATA = "resources/metro.csv"
private const val COUNTRY_DATA = "resources/countries.csv"
private const val CANADA_DATA = "resources/canada.csv"
private const val CHINA_DATA = "resources/china.csv"
private const val AUSTRALIA_DATA = "resources/australia.csv"

sealed class PopulationLookupData(resource: String): (String) -> Long? {
    val dataLines = CountyData::class.java.getResource(resource).csvLines().toList()
    val dataTable: MutableMap<String, Long> = mutableMapOf()
}

object CountyData: PopulationLookupData(COUNTY_CENSUS_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[12].replace(",", "").toLong() } }

    override fun invoke(input: String): Long? {
        val split = input.split(", ", ",")
        if (split.size != 2 && split.size != 3) {
            return null
        }

        // account for all the possible suffices for counties in the US
        return dataTable[input.removeSuffix(", US").toLowerCase()]
                ?: dataTable["${split[0]}, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} county, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} parish, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} city, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} municipality, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} borough, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} city and borough, ${split[1]}".toLowerCase()]
                ?: dataTable["${split[0]} census area, ${split[1]}".toLowerCase()]
    }
}

object StateData: PopulationLookupData(STATE_CENSUS_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[12].replace(",", "").toLong() } }
    override fun invoke(input: String) = when (input.split(", ", ",").size) {
        2 -> dataTable[input.removeSuffix(", US").toLowerCase()]
        1 -> dataTable[input.toLowerCase()]
        else -> null
    }
}

object MetroData: PopulationLookupData(METRO_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[1].toLong() } }
    override fun invoke(input: String) = dataTable[input.toLowerCase()]
}

object CanadaProvinceData: PopulationLookupData(CANADA_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[5].replace(",", "").toLong() } }
    override fun invoke(input: String) = dataTable[input.removeSuffix(", Canada").toLowerCase()]
}

object AustraliaData: PopulationLookupData(AUSTRALIA_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[1].toLong() } }
    override fun invoke(input: String) = dataTable[input.removeSuffix(", Australia").toLowerCase()]
}

object ChinaData: PopulationLookupData(CHINA_DATA) {
    init { dataLines.forEach { dataTable[it[0].toLowerCase()] = it[1].replace(",", "").toLong() } }
    override fun invoke(input: String) = dataTable[input.removeSuffix(", China").toLowerCase()]
}

object CountryData: PopulationLookupData(COUNTRY_DATA) {
    init { dataLines.forEach { dataTable[it[1].toLowerCase()] = it[2].replace(",", "").toLong() } }

    override fun invoke(input: String) = when {
        input.endsWith(", France") -> dataTable[input.removeSuffix(", France").toLowerCase()]
        input.endsWith(", United Kingdom") -> dataTable[input.removeSuffix(", United Kingdom").toLowerCase()]
        input.endsWith(", Netherlands") -> dataTable[input.removeSuffix(", Netherlands").toLowerCase()]
        input.endsWith(", Denmark") -> dataTable[input.removeSuffix(", Denmark").toLowerCase()]
        input.endsWith(", US") -> dataTable[input.removeSuffix(", US").toLowerCase()]
        else -> dataTable[input.toLowerCase()]
    }
}