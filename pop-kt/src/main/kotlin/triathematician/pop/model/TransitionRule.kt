package triathematician.pop.model

import triathematician.util.sampleBinomial
import kotlin.random.Random

/** A transition rule computes a sample that transitions from one cohort to another. */
abstract class TransitionRule(val from: Cohort, val to: Cohort) {
    abstract fun sample(popSize: Int): Int
}

/**
 * Defines a transition rule where each individual has an equal chance of moving from the source cohort to the target.
 * The probability of movement is ``p*|from|``.
 */
class ChanceTransition(from: Cohort, to: Cohort, val p: Double): TransitionRule(from, to) {
    /** Sample uniformly from the source cohort with likelihood p to get # of transitions. */
    override fun sample(popSize: Int) = Random.sampleBinomial(from.count, p)
}

/**
 * Defines a transition rule where each individual has a chance of moving proportional to the percentage of the target population.
 * The probability of movement is p*|cohort1|*|cohort2|/|pop|.
 * The movement may occur between the cohorts used in this computation or other cohorts.
 */
class ChanceEncounterTransition(val cohort1: Cohort, val cohort2: Cohort, from: Cohort, to: Cohort, val p: Double): TransitionRule(from, to) {
    /** Sample uniformly from the source cohort with likelihood p*|cohort2|/|pop| to get # of transitions. */
    override fun sample(n: Int) = Random.sampleBinomial(cohort1.count, p * cohort2.count/n.toDouble())
}