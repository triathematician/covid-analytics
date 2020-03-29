package triathematician.pop.model.impl

import triathematician.pop.model.chanceEncounter
import triathematician.pop.model.cohort
import triathematician.pop.model.compartmentModel
import triathematician.pop.model.transition
import kotlin.math.max

/** Model representing susceptible-infected-recovered. */
fun seir(initialPop: Int = 1000000, initialExposed: Int = 5, initialInfected: Int = 5, initialRecovered: Int = 0, mixRate: Double = 0.25, infectionRate: Double = 0.05, recoverRate: Double = 0.05) = compartmentModel {
    cohort("susceptible", initialPop - initialExposed - initialInfected - initialRecovered)
    cohort("exposed", initialExposed)
    cohort("infectious", initialInfected)
    cohort("recovered", initialRecovered)
    chanceEncounter(first = "susceptible", second = "infectious", to = "exposed", weight = mixRate)
    transition(from = "exposed", to = "infectious", probability = infectionRate)
    transition(from = "infectious", to = "recovered", probability = recoverRate)
}

fun main() {
    with (seir()) {
        var peakInfected = cohort("infectious").count
        println("---")
        for (i in 1..100) {
            println("$i ${cohort("susceptible").count} ${cohort("exposed").count} ${cohort("infectious").count} ${cohort("recovered").count}")
            iterate()
            peakInfected = max(peakInfected, cohort("infectious").count)
        }
        println("101 ${cohort("susceptible").count} ${cohort("exposed").count} ${cohort("infectious").count} ${cohort("recovered").count}")
        println("---")
        println("Peak Infections: $peakInfected")
    }
}