package triathematician.pop.model2

import triathematician.pop.model.Cohort
import triathematician.util.distinctInts
import kotlin.random.Random

/**
 * This models a population as a set of cohorts.
 * Each cohort represents either a single actor or a group that are modeled identically.
 * The population has indices ``0 until count``, and each index maps to a specific individual in a specific cohort.
 */
data class Population(var cohorts: List<Cohort>) {

    /** Construct with varargs cohorts */
    constructor(vararg cohorts: Cohort): this(listOf(*cohorts))
    /** Construct with just a single total number. */
    constructor(total: Int): this(listOf(Cohort("everyone", total)))

    val count: Int
        get() = cohorts.sumBy { it.count }

    /** Get the cohort for the given individual. */
    fun cohortOf(i: Int): Cohort {
        var maxInThisCohort = -1
        for (c in cohorts) {
            maxInThisCohort += c.count
            if (i <= maxInThisCohort) {
                return c
            }
        }
        throw IllegalArgumentException()
    }

}

/** Create population for SIR model, with three cohorts. */
fun sirModelPopulation(initialSusceptible: Int, initialInfected: Int, initialRecovered: Int): Population {
    val cohortS = Cohort("Susceptible", initialSusceptible)
    val cohortI = Cohort("Infected", initialInfected)
    val cohortR = Cohort("Recovered", initialRecovered)
    return Population(cohortS, cohortI, cohortR)
}

/**
 * Generates a random pair of cohorts based on population sizes.
 * Prohibits sampling twice from a cohort of size 1.
 */
fun Population.randomPair(r: Random = Random): Pair<Cohort, Cohort> {
    var cohort1: Cohort
    var cohort2: Cohort
    do {
        val ints = r.distinctInts(n = 2, min = 0, max = count - 1)
        cohort1 = cohortOf(ints[0])
        cohort2 = cohortOf(ints[1])
    } while (cohort1 != cohort2 || cohort1.count > 1)
    return cohort1 to cohort2
}

/**
 * Generate sequence of random pairs from the population.
 * Each pair is a random sample of two individuals from the population.
 */
fun Population.randomPairSequence(r: Random = Random) = generateSequence { randomPair(r) }