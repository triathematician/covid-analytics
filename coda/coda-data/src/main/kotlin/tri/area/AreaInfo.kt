package tri.area

/** Information about a region. */
data class AreaInfo(var id: String, var type: RegionType, var parent: String, var fips: Int? = null,
                    var population: Long? = null, var latitude: Float? = null, var longitude: Float? = null)

/** Construct representation of a zipcode. */
fun zipcodeRegion(zipcode: Int) = AreaInfo(zipcode.toString(), RegionType.ZIPCODE, "")

/** Region type. */
enum class RegionType(vararg parentTypes: RegionType) {
    GLOBAL(),
    CONTINENT(GLOBAL),
    COUNTRY_REGION(GLOBAL, CONTINENT),
    PROVINCE_STATE(COUNTRY_REGION),
    METRO(PROVINCE_STATE, COUNTRY_REGION),
    COUNTY(METRO, PROVINCE_STATE),
    ZIPCODE(COUNTY, METRO, PROVINCE_STATE),
    UNKNOWN(PROVINCE_STATE, COUNTRY_REGION, CONTINENT, GLOBAL)
}