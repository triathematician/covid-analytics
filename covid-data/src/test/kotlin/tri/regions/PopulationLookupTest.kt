package tri.regions

import tri.util.csvKeyValues

fun main() {
    println(PopulationLookup("New York City, New York, US"))
    println(PopulationLookup("New York-Newark-Jersey City, NY-NJ-PA, US"))
    println(PopulationLookup("New York, US"))

    println(PopulationLookup("New York, New York, US"))
    println(PopulationLookup.fips(36061))

    println("\n--")
    UnitedStates.cbsas.first { it.cbsaTitle == "New York-Newark-Jersey City, NY-NJ-PA" }.let { println(it.population) }

    UnitedStates::class.java.getResource("resources/census-cbsa-fips.csv").csvKeyValues().toList()
            .filter { it["cbsatitle"] == "New York-Newark-Jersey City, NY-NJ-PA" }
            .onEach {
                println("${it["fipscountycode"]}, ${it["countycountyequivalent"]}, " +
                        "${PopulationLookup(it["countycountyequivalent"]!!.removeSuffix(" County") + ", " + it["statename"])}")
            }

    UnitedStates::class.java.getResource("resources/census-cbsa-fips.csv").csvKeyValues()
            .map { CbsaInfo(it["cbsacode"]!!.toInt(), it["csacode"]?.toIntOrNull(), it["cbsatitle"]!!, it["csatitle"]!!,
                    it["cbsatitle"]!!.substringAfter(", "), listOf(it["fipsstatecode"]!!.toInt()*1000 + it["fipscountycode"]!!.toInt())) }
            .filter { it.cbsaCode == 35620 }.toList()
            .onEach {
                it.counties.forEach {
                    println("$it, ${PopulationLookup.fips(it)}")
                }
            }
}