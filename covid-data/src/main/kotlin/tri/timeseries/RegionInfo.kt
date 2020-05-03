package tri.timeseries

/** Information about a region. */
data class RegionInfo(var id: String, var type: RegionType, var parent: String, var fips: Int? = null,
                      var population: Long? = null, var latitude: Float? = null, var longitude: Float? = null)

/** Region type. */
enum class RegionType {
    GLOBAL,
    COUNTRY_REGION,
    PROVINCE_STATE,
    COUNTY,
    METRO,
    UNKNOWN
}