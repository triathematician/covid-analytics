package tri.area

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import tri.util.csvResource

/** Quick access to looking up areas/elements by region name. */
object Lookup {

    //region CACHE

    private val areaCache = mutableMapOf<String, AreaInfo>().apply {
        listOf("US", "USA", "United States", "United States of America").forEach { this[it] = USA }
        listOf("Earth", "World").forEach { this[it] = EARTH }
        Usa.states.forEach {
            this[it.key] = it.value
            this["${it.value.fullName}, US"] = it.value
        }
    }
    private val notFound = mutableMapOf<String, AreaInfo>()

    //endregion

    //region PRIMARY LOOKUP

    /** Get object for area with given name. */
    fun area(lookupName: String): AreaInfo {
        val name = aliases[lookupName] ?: lookupName
        areaCache[name]?.let { return it }
        notFound[name]?.let { return it }

        val region = JhuAreaData.lookupCaseInsensitive(name)?.toAreaInfo()
        return if (region == null) {
            println("Area not found: $name")
            notFound[name] = UNKNOWN
            UNKNOWN
        } else {
            areaCache[name] = region
            region
        }
    }

    /** Get object for area with given FIPS. */
    fun areaByFips(fips: Int) = Usa.counties[fips] ?: Usa.cbsas[fips]

    //endregion

    /** Get FIPS for area with given name. */
    fun fips(name: String) = area(name).fips!!

    /** Get population for area with given name. */
    fun population(name: String) = area(name).metrics.population

    //region CHECKS

    private val aliases = Lookup::class.csvResource<AreaAlias>("resources/area-aliases.csv")
            .map { it.alias to it.name }.toMap()

    private class AreaAlias(val alias: String, val name: String)

    //endregion

}

///** Lookup region by FIPS code. */
//fun fips(fips: Int): AreaInfo? = UnitedStates.fipsToCounty(fips) ?: UnitedStates.countyFipsToCbsaRegion(fips)
//object PopulationLookup: (String) -> Long? {
//    fun fips(fips: Int): Long? = JhuAreaData.fips(fips)?.pop
//
//    override fun invoke(id: String): Long? {
//        if (id == GLOBAL) return GLOBAL_POPULATION
//        val lookupId = alias(id)
//        MetroData(lookupId)?.let { return it }
//        CountyData(lookupId)?.let { return it }
//        CanadaProvinceData(lookupId)?.let { return it }
//        ChinaData(lookupId)?.let { return it }
//        AustraliaData(lookupId)?.let { return it }
//        StateData(lookupId)?.let { return it }
//        CountryData(lookupId)?.let { return it }
//        logIfNotFound(lookupId)
//        return null
//    }
//}
//
//
//private val loggedIds = mutableSetOf<String>()
//private val excludedIds = listOf("Unassigned", "Out-of", "Out of", "Recovered", "Cruise", "Princess", "Evacuee")
//
//private fun logIfNotFound(id: String) {
//    if (excludedIds.none { it in id } && id !in loggedIds) {
////        println("no pop for $id")
//        loggedIds += id
//    }
//}