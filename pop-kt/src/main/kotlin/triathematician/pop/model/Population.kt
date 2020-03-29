package triathematician.pop.model

/** Models a group of individuals in a population with fixed characteristics. */
data class Cohort(var name: String, var count: Int, var about: Map<String, Any> = mutableMapOf())

fun List<Cohort>.populationArray() = map { it.count }.toIntArray()