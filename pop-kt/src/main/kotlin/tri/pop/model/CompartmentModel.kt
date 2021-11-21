package tri.pop.model

/**
 * A compartment model for epidemiology models the spread of a disease based on characteristics of individuals in separate "compartments".
 * We use the term "cohort" rather than "compartment".
 * This class manages the cohorts associated with this kind of model and the coefficients for moving elements between the cohorts.
 */
class CompartmentModel(_cohorts: List<Cohort>) {

    /** The cohorts in the model. Updated during each simulation. */
    val cohorts: MutableList<Cohort> = _cohorts.toMutableList()
    /** Initial population sizes per cohort. */
    val initialSizes = mutableMapOf<Cohort, Int>()
    /** Rules for transitioning from one step to the next. */
    val rules = mutableListOf<TransitionRule>()

    //region LOOKUPS

    fun cohort(s: String): Cohort = cohorts.find { it.name == s } ?: throw IllegalArgumentException()

    //endregion

    fun reset() {
        cohorts.forEach { it.count = initialSizes[it] ?: 0 }
    }

    fun iterate() {
        val popSize = cohorts.sumBy { it.count }
        val delta = cohorts.map { it to 0 }.toMap().toMutableMap()
        do {
            rules.forEach { rule ->
                val n = rule.sample(popSize)
                delta.merge(rule.from, -n) { t, u -> t + u }
                delta.merge(rule.to, n) { t, u -> t + u }
            }
        } while (!delta.all { it.key.count + it.value >= 0 })
        delta.forEach { (c, i) -> c.count += i}
    }

}

//region DSL BUILDERS

fun compartmentModel(builder: CompartmentModel.() -> Unit) = CompartmentModel(emptyList()).also(builder)

fun CompartmentModel.cohort(name: String, initialSize: Int) {
    val c = Cohort(name, initialSize)
    cohorts.add(c)
    initialSizes[c] = initialSize
}

fun CompartmentModel.transition(from: String, to: String, probability: Double) {
    rules.add(ChanceTransition(cohort(from), cohort(to), probability))
}

fun CompartmentModel.chanceEncounter(first: String, second: String, from: String = first, to: String = second, weight: Double) {
    rules.add(ChanceEncounterTransition(cohort(first), cohort(second), cohort(from), cohort(to), weight))
}

//endregion

//region EXECUTORS

fun CompartmentModel.run(steps: Int = 100): MutableList<IntArray> {
    val result = mutableListOf<IntArray>()
    result.add(cohorts.populationArray())
    for (i in 1..steps) {
        iterate()
        result.add(cohorts.populationArray())
    }
    return result
}

//endregion