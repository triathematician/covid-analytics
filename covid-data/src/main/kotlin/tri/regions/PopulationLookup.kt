package tri.regions

const val GLOBAL = "Global"
const val GLOBAL_POPULATION = 7775510000

fun lookupPopulation(id: String): Long? {
    if (id == GLOBAL) return GLOBAL_POPULATION
    val lookupId = alias(id)
    MetroData(lookupId)?.let { return it }
    CountyData(lookupId)?.let { return it }
    CanadaProvinceData(lookupId)?.let { return it }
    ChinaData(lookupId)?.let { return it }
    AustraliaData(lookupId)?.let { return it }
    StateData(lookupId)?.let { return it }
    CountryData(lookupId)?.let { return it }
    logIfNotFound(lookupId)
    return null
}

private fun alias(id: String) = when (id) {
    "Burma" -> "Myanmar"
    "Congo (Kinshasa)" -> "Congo"
    "Cote d'Ivoire" -> "Côte d'Ivoire"
    "Curacao" -> "Curaçao"
    "Curacao, Netherlands" -> "Curaçao"
    "Czechia" -> "Czech Republic (Czechia)"
    "Faroe Islands" -> "Faeroe Islands"
    "Faroe Islands, Denmark" -> "Faeroe Islands"
    "Korea, South" -> "South Korea"
    "Reunion" -> "Réunion"
    "Reunion, France" -> "Réunion"
    "Saint Kitts and Nevis" -> "Saint Kitts & Nevis"
    "Saint Vincent and the Grenadines" -> "St. Vincent & Grenadines"
    "St Martin" -> "Saint Martin"
    "St Martin, France" -> "Saint Martin"
    "Taiwan*" -> "Taiwan"
    "Turks and Caicos Islands" -> "Turks and Caicos"
    "Turks and Caicos Islands, United Kingdom" -> "Turks and Caicos"
    "US" -> "United States"
    "Virgin Islands, US" -> "U.S. Virgin Islands"
    else -> id
}

private val loggedIds = mutableSetOf<String>()
private val excludedIds = listOf("Unassigned", "Out-of", "Out of", "Recovered", "Cruise", "Princess", "Evacuee")

private fun logIfNotFound(id: String) {
    if (excludedIds.none { it in id } && id !in loggedIds) {
//        println("no pop for $id")
        loggedIds += id
    }
}