package tri.area

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Key information associated with a geographic area or region. The design of this library assumes there is a unique ID
 * for every area so these can be identified uniquely, so the id is expected to be a comma-delimited set of identifiers
 * that together make it uniquely defined.
 */
open class AreaInfo(val id: String, val type: RegionType, @JsonIgnore val parent: AreaInfo?, val fips: Int? = null, val metrics: AreaMetrics) {
    init {
        require(if (parent == null) type.parents.isEmpty() else parent.type in type.parents) { "Parent type of $id was invalid: $type cannot have parent $parent" }
    }

    @get:JsonIgnore
    val population
        get() = metrics.population

    override fun toString(): String {
        return "AreaInfo(id='$id', type=$type)"
    }
}

//region UNIQUE AREAS

val EARTH = AreaInfo("Earth", RegionType.PLANET, null, null, AreaMetrics(7775510000L))
//val NORTH_AMERICA = AreaInfo("North America", RegionType.CONTINENT, EARTH, null, TODO())
val USA = AreaInfo("United States", RegionType.COUNTRY_REGION, EARTH, null, AreaMetrics(-1L))
val UNKNOWN = AreaInfo("Unknown", RegionType.UNKNOWN, EARTH, null, AreaMetrics(0L))

//endregion

/** Region type. */
enum class RegionType(vararg parentTypes: RegionType) {
    PLANET,
    CONTINENT(PLANET),
    COUNTRY_REGION(PLANET, CONTINENT),
    PROVINCE_STATE_AGGREGATE(COUNTRY_REGION),
    PROVINCE_STATE(PROVINCE_STATE_AGGREGATE, COUNTRY_REGION),
    METRO(PROVINCE_STATE, COUNTRY_REGION),
    COUNTY(METRO, PROVINCE_STATE),
    ZIPCODE(COUNTY, METRO, PROVINCE_STATE),
    UNKNOWN(PROVINCE_STATE, COUNTRY_REGION, CONTINENT, PLANET);

    val parents = listOf(*parentTypes)
}