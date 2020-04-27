package tri.pop.model.impl

import tri.pop.model.chanceEncounter
import tri.pop.model.cohort
import tri.pop.model.compartmentModel
import tri.pop.model.transition
import kotlin.math.max

/** Model representing susceptible-infected-recovered. */
fun sis() = compartmentModel {
    cohort("susceptible", 1000000 - 5)
    cohort("infected", 5)
    chanceEncounter(first = "susceptible", second = "infected", weight = 0.25)
    transition(from = "infected", to = "susceptible", probability = 0.15)
}

fun main() {
    with (sis()) {
        var peakInfected = cohort("infected").count
        println("---")
        for (i in 1..1000) {
            println("$i ${cohort("susceptible").count} ${cohort("infected").count}")
            peakInfected = max(peakInfected, cohort("infected").count)
            iterate()
        }
        println("---")
        println("Peak Infections: $peakInfected")
    }
}