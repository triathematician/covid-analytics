package triathematician.pop.model.impl

import triathematician.pop.model.chanceEncounter
import triathematician.pop.model.cohort
import triathematician.pop.model.compartmentModel
import triathematician.pop.model.transition
import kotlin.math.max

/** Model representing susceptible-infected-recovered. */
fun sir(initialPop: Int = 1000000, initialInfected: Int = 5, initialRecovered: Int = 0, mixRate: Double = 0.25, recoverRate: Double = 0.05) = compartmentModel {
    cohort("susceptible", initialPop - initialInfected - initialRecovered)
    cohort("infected", initialInfected)
    cohort("recovered", initialRecovered)
    chanceEncounter(first = "susceptible", second = "infected", weight = mixRate)
    transition(from = "infected", to = "recovered", probability = recoverRate)
}

fun main() {
    with (sir()) {
        var peakInfected = cohort("infected").count
        println("---")
        for (i in 1..100) {
            println("$i ${cohort("susceptible").count} ${cohort("infected").count} ${cohort("recovered").count}")
            iterate()
            peakInfected = max(peakInfected, cohort("infected").count)
        }
        println("101 ${cohort("susceptible").count} ${cohort("infected").count} ${cohort("recovered").count}")
        println("---")
        println("Peak Infections: $peakInfected")
    }
}