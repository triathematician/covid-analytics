package tri.area

fun main() {
    println(Lookup.population("New York City, New York, US"))
    println(Lookup.population("New York-Newark-Jersey City, NY-NJ-PA, US"))
    println(Lookup.population("New York, US"))

    println(Lookup.population("New York, New York, US"))
    println(Lookup.areaByFips(36061))

    println("\n--")
    Lookup.area("New York-Newark-Jersey City, NY-NJ-PA").let { println(it.population) }
    (Lookup.area("New York-Newark-Jersey City, NY-NJ-PA") as UsCbsaInfo).let {
        it.counties.forEach {
            println("${it.fips}, ${it.id}, ${it.population}")
        }
    }
}