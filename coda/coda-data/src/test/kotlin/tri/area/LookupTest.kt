package tri.area

import org.junit.Test

fun main() {
    println(Lookup.area("Los Angeles, California, US"))
}

class LookupTest {

    @Test
    fun testLookups() {
        println(Lookup.area("Los Angeles, CA, US"))
        println(Lookup.area("Los Angeles, California, US"))
        println((Lookup.area("Los Angeles, California, US") as UsCountyInfo).cbsa)
        println(Lookup.area("St. Louis City, Missouri, US"))
        println(Lookup.area("Virginia Beach city, Virginia, US"))
        println(Lookup.area("Virginia Beach, Virginia, US"))
    }

    @Test
    fun testPopulations() {
        println(Lookup.population("Los Angeles, CA, US"))

        println(Lookup.population("New York City, New York, US"))
        println(Lookup.population("New York-Newark-Jersey City, NY-NJ-PA, US"))
        println(Lookup.population("New York, US"))

        println(Lookup.population("New York, New York, US"))
        println(Lookup.areaByFips(36061))

        println("\n--")
        println(Lookup.area("New York-Newark-Jersey City, NY-NJ-PA").population)
        Lookup.cbsa("New York-Newark-Jersey City, NY-NJ-PA").let {
            it.counties.forEach {
                println("${it.fips}, ${it.id}, ${it.population}")
            }
        }
    }
}