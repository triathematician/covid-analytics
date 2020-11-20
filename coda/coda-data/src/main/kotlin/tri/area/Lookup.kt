package tri.area

import tri.util.csvResource
import tri.util.javaTrim
import tri.util.warning
import java.nio.charset.Charset

/**
 * Quick access to looking up areas/elements by region name. Maintains a cache so that only one lookup is performed per
 * request string.
 */
object Lookup {

    //region CACHE

    private val areaCache = mutableMapOf<String, AreaInfo>().apply {
        listOf("US", "USA", "United States", "United States of America").forEach { this[it] = USA }
        listOf("Earth", "World").forEach { this[it] = EARTH }
        // prepopulate with state abbreviation "OH" and full name "Ohio, US"
        //   -- do not use name without US suffix because e.g. Georgia is ambiguous
        Usa.states.forEach { (abbrev, state) ->
            this[abbrev] = state
            this["${state.fullName}, US"] = state
        }
        // prepopulate with FEMA/HHS regions
        Usa.femaRegions.forEach { (num, area) ->
            this[area.id] = area
        }
        // prepopulate with Census regions
        Usa.censusRegions.forEach { (num, area) ->
            this[area.id] = area
        }
        // prepopulate with county FIPS and combined id, e.g. "Cook, Illinois, US"
        Usa.counties.forEach {
            this[it.key.toString()] = it.value
            this[it.value.id] = it.value
        }
        // prepopulate with CBSA ids and titles
        Usa.cbsas.forEach {
            this[it.key.toString()] = it.value
            this[it.value.id] = it.value
            this["${it.value.id}, US"] = it.value
        }
        // prepopulate with e.g. "Unassigned, Ohio, US"
        Usa.unassigned.forEach {
            this["Unassigned, ${Usa.statesByAbbreviation[it.key]}, US"] = it.value
        }
    }
    private val notFound = mutableMapOf<String, AreaInfo>()

    //endregion

    //region PRIMARY LOOKUP

    /** Add area to lookup cache. */
    fun addArea(area: AreaInfo) {
        if (area.id !in areaCache.keys)
            areaCache[area.id] = area
    }

    /**
     * Get object for area with given name. Logs an error and returns a generic "Unknown" area if not found.
     * @param lookupName name to lookup
     * @param assumeUsState if true, lookup will assume the area is part of the USA if not found or ambiguous
     */
    fun area(lookupName: String, assumeUsState: Boolean = false) = areaOrNull(lookupName, assumeUsState) ?: UNKNOWN

    /** Get object for area with given name. Logs an error and returns null if not found. */
    fun areaOrNull(lookupName: String, assumeUsState: Boolean = false): AreaInfo? {
        val name = aliases[lookupName.javaTrim()] ?: lookupName
        val altName = if (assumeUsState) "$name, US" else null
        areaCache[name]?.let { return it }
        areaCache[altName]?.let { return it }
        notFound[name]?.let { return null }

        val jhuArea = JhuAreaData.lookupCaseInsensitive(name) ?: JhuAreaData.lookupCaseInsensitive(altName ?: "")
        val areaInfo = if (jhuArea?.fips != null) Usa.counties[jhuArea.fips] else jhuArea?.toAreaInfo()
        return if (areaInfo == null) {
            println(Charset.defaultCharset())
            warning<Lookup>("Area not found: $name")
            warning<Lookup>(name.map { it.toInt() }.toString())
            notFound[name] = UNKNOWN
            null
        } else {
            areaCache[name] = areaInfo
            areaInfo
        }
    }

    /** Get object for area with given FIPS. */
    fun areaByFips(fips: Int) = Usa.counties[fips] ?: Usa.cbsas[fips]

    //endregion

    /** Get FIPS for area with given name. */
    fun fips(name: String) = area(name).fips!!

    /** Get population for area with given name. */
    fun population(name: String) = area(name).metrics.population

    /** Lookup area based on CBSA name. */
    fun cbsa(name: String) = Usa.cbsaByName[name]!!

    //region CHECKS

    private val aliases = Lookup::class.csvResource<AreaAlias>(true, "resources/area-aliases.csv")
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