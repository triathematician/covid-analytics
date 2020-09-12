package tri.area

/** Metrics associated with a geographic region. */
class AreaMetrics(
        val population: Long? = null,
        val latitude: Float? = null,
        val longitude: Float? = null) {

    companion object {
        fun aggregate(areas: List<AreaInfo>) = AreaMetrics(areas.totalPopulation)
    }
}

/** Sums area populations. */
val List<AreaInfo>.totalPopulation
        get() = sumByDouble { it.population?.toDouble() ?: 0.0 }.toLong()