package tri.area

import com.fasterxml.jackson.annotation.JsonProperty
import tri.util.csvResource

/** Loads JHU region/population data. */
internal object JhuAreaData {
    val data = JhuAreaData::class.csvResource<JhuAreaInfo>("resources/jhucsse/jhu-iso-fips-lookup.csv")
    val index = data.groupByOne { it.indexKey }
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

    /** Get unique key used to lookup this region. */
    val indexKey: Any
        get() = when {
            fips == null -> combinedKey
            fips < 100 -> Usa.abbreviationsByState[provinceOrState]!!
            else -> fips
        }

    val regionType
        get() = when {
            admin2.isEmpty() && provinceOrState.isEmpty() -> RegionType.COUNTRY_REGION
            provinceOrState.isEmpty() -> RegionType.METRO
            admin2.isEmpty() -> RegionType.PROVINCE_STATE
            else -> RegionType.COUNTY
        }

    val regionParent
        get() = when {
            admin2.isEmpty() && provinceOrState.isEmpty() -> EARTH
            provinceOrState.isEmpty() || admin2.isEmpty() -> Lookup.area(countryOrRegion)
            else -> Lookup.area("$provinceOrState, $countryOrRegion")
        }

    /** Convert to general area info object. */
    fun toAreaInfo(): AreaInfo {
        require(fips == null) { "Use Usa object to access areas within the US." }
        return AreaInfo(combinedKey, regionType, regionParent, fips, AreaMetrics(population, latitude, longitude))
    }
}

//region UTILS

private fun <X, Y> List<X>.groupByOne(map: (X) -> Y) = groupBy(map).mapValues {
    if (it.value.size > 1) {
        println("Duplicate keys: ${it.value}")
    }
    it.value.first()
}

//endregion