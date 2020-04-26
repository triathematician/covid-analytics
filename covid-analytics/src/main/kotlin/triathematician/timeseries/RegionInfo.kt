package triathematician.timeseries;

/** Information about a region. */
data class RegionInfo(var id: String, var fips: Int?, var name: String, var population: Long? = null, var latitude: Float? = null, var longitude: Float? = null) {
    constructor(id: String): this(id, null, id)
}

/** Region type. */
enum class RegionType {
    NATION,
    STATE,
    COUNTY,
    CITY
}