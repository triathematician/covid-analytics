package tri.area

import tri.util.csvKeyValues

fun main() {
    println(PopulationLookup("New York City, New York, US"))
    println(PopulationLookup("New York-Newark-Jersey City, NY-NJ-PA, US"))
    println(PopulationLookup("New York, US"))

    println(PopulationLookup("New York, New York, US"))
    println(PopulationLookup.fips(36061))

    println("\n--")
    UnitedStates.cbsas.first { it.cbsaTitle == "New York-Newark-Jersey City, NY-NJ-PA" }.let { println(it.population) }

    UnitedStates::class.java.getResource("resources/Mar2020cbsaDelineation.csv").csvKeyValues().toList()
            .filter { it["CBSA Title"] == "New York-Newark-Jersey City, NY-NJ-PA" }
            .onEach {
                println("${it["FIPS County Code"]}, ${it["County/County Equivalent"]}, " +
                        "${PopulationLookup(it["County/County Equivalent"]!!.removeSuffix(" County") + ", " + it["State Name"])}")
            }

    UnitedStates::class.java.getResource("resources/Mar2020cbsaDelineation.csv").csvKeyValues()
            .map { CbsaInfo(it["CBSA Code"]!!.toInt(), it["CSA Code"]?.toIntOrNull(), it["CBSA Title"]!!, it["CSA Title"]!!,
                    it["CBSA Title"]!!.substringAfter(", "), listOf(it["FIPS State Code"]!!.toInt()*1000 + it["FIPS County Code"]!!.toInt())) }
            .filter { it.cbsaCode == 35620 }.toList()
            .onEach {
                it.counties.forEach {
                    println("$it, ${PopulationLookup.fips(it)}")
                }
            }
}