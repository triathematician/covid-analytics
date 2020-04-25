package triathematician.population

/** Provides information about states and counties in the US. */
object UnitedStates {
    val states: List<State> by lazy { JhuRegionData.usStates.map { State(it.region2, it.fips!!, it.region2, it.pop!!) } }
    val stateNames: List<String> by lazy { states.map { it.name } }
}

class State(var id: String, var fips: Int, var name: String, var population: Long)