package tri.area

import com.fasterxml.jackson.annotation.JsonProperty
import tri.util.csvResource

/** Loads JHU region/population data. */
internal object JhuAreaData {
    private val data = JhuAreaData::class.csvResource<JhuAreaInfo>("resources/jhucsse/jhu-iso-fips-lookup.csv")

    val index = data.groupByOne { it.indexKey }
    val areas = index.values

    private val lowerIndex by lazy { index.mapKeys { it.key.toString().toLowerCase() } }

    /** Looks up area by name, case-insensitive. */
    fun lookupCaseInsensitive(key: Any) = index.getOrElse(key) { lowerIndex[key.toString().trim().toLowerCase()] }
}

/** Data structure provided by JHU region data. */
internal data class JhuAreaInfo(val UID: Int, val iso2: String, val iso3: String, var code3: Int,
                       @JsonProperty("FIPS") val fips: Int? = null, @JsonProperty("Admin2") val admin2: String,
                       @JsonProperty("Province_State") val provinceOrState: String, @JsonProperty("Country_Region") val countryOrRegion: String,
                       @JsonProperty("Lat") val latitude: Float, @JsonProperty("Long_") val longitude: Float,
                       @JsonProperty("Combined_Key") val combinedKey: String, @JsonProperty("Population") val population: Long) {

    /** Get unique key used to lookup this region. Regions with FIPS have more than one possible key. */
    val indexKey: List<Any>
        get() = when {
            fips == null -> listOf(combinedKey)
            fips < 100 -> listOf(fips, Usa.abbreviationsByState[provinceOrState]!!)
            else -> listOf(fips, combinedKey)
        }

    val regionType
        get() = when {
            admin2.isEmpty() && provinceOrState.isEmpty() -> AreaType.COUNTRY_REGION
            provinceOrState.isEmpty() -> AreaType.METRO
            admin2.isEmpty() -> AreaType.PROVINCE_STATE
            else -> AreaType.COUNTY
        }

    val regionParent
        get() = when {
            admin2.isEmpty() && provinceOrState.isEmpty() -> EARTH
            provinceOrState.isEmpty() || admin2.isEmpty() -> Lookup.areaOrNull(countryOrRegion)!!
            else -> Lookup.areaOrNull("$provinceOrState, $countryOrRegion")!!
        }

    /** Convert to general area info object. */
    fun toAreaInfo(): AreaInfo {
        require(fips == null || fips >= 80000) { "Use Usa object to access areas within the US: $this" }
        return AreaInfo(combinedKey, regionType, regionParent, fips, AreaMetrics(population, latitude, longitude))
    }
}

//region UTILS

private fun <X, Y> List<X>.groupByOne(keySelectors: (X) -> List<Y>): Map<Y, X> {
    val res = mutableMapOf<Y, MutableList<X>>()
    for (element in this) {
        keySelectors(element).forEach { key ->
            val list = res.getOrPut(key) { ArrayList() }
            list.add(element)
        }
    }
    res.values.forEach { if (it.size > 1) println("Two values had the same key: $it") }
    return res.mapValues { it.value.first() }
}

//endregion