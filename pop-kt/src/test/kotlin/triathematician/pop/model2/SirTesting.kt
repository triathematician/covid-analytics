package triathematician.pop.model2

import triathematician.util.distinctInts
import triathematician.util.sampleBinomial
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

data class SirModel(var pop: Population, var evolve: SirEvolve)
data class SirState(var susceptible: Int, var infected: Int, var recovered: Int) {
    val population: Int
        get() = susceptible + infected + recovered
}
typealias SirEvolve = (SirState) -> SirState

//region ODE Model

/** ODE Model, Beta = [infectionRate], Gamma = [recoveryRate]. */
data class SirEvolveOde(var infectionRate: Double, var recoveryRate: Double, var timeStep: Double): SirEvolve {
    override operator fun invoke(current: SirState): SirState {
        val newInfected = min(current.susceptible, (infectionRate * current.infected * current.susceptible * timeStep).toInt())
        val newRecovered = (recoveryRate * current.infected * timeStep).toInt()
        return SirState(current.susceptible - newInfected, current.infected + newInfected - newRecovered, current.recovered + newRecovered)
    }
}

fun SirModel.runOde(infected: Int, immune: Int) {
    var time = 0.0
    var state = SirState(pop.count - infected - immune, infected, immune)
    var peakInfected = infected
    val ev = evolve as SirEvolveOde
    val initialSusceptible = state.susceptible
    println("R_0 = ${(state.susceptible * ev.infectionRate / ev.recoveryRate)}")
    println("---")
    for (i in 1..100) {
        println("${time.format(3)} ${state.susceptible} ${state.infected} ${state.recovered}")
        state = evolve(state)
        time += ev.timeStep
        peakInfected = max(peakInfected, state.infected)
    }
    println("${time.format(3)} ${state.susceptible} ${state.infected} ${state.recovered}")
    println("---")
    println("Initial R_0 = ${(initialSusceptible * ev.infectionRate / ev.recoveryRate).format(3)}, Peak Infections: $peakInfected")
}

//endregion

//region Population Mixing Model

/**
 * Mixing model, based on random interactions with population.
 * Picks n random population pairs at each time step.
 * If the pair is "IS", the susceptible element is infected.
 * After this step, a certain percentage of the population recovers.
 */
data class SirMixALot2(var n: Int, var infectionRate: Double, var recoveryRate: Double): SirEvolve {
    override operator fun invoke(current: SirState): SirState {
        val pop = sirModelPopulation(current.susceptible, current.infected, current.recovered)
        val newInfected = pop.randomPairSequence()
                .take(n)
                .filter { setOf(it.first, it.second) == setOf(pop.cohorts[0], pop.cohorts[1]) }
                .count()
        val newRecovered = Random.sampleBinomial(current.infected, recoveryRate)
        return SirState(current.susceptible - newInfected, current.infected + newInfected - newRecovered, current.recovered + newRecovered)
    }
}

/**
 * Mixing model, based on random interactions with population.
 * Picks n random population pairs at each time step.
 * If the pair is "IS", the susceptible element is infected.
 * After this step, a certain percentage of the population recovers.
 */
data class SirMixALot(var n: Int, var infectionRate: Double, var recoveryRate: Double): SirEvolve {
    override operator fun invoke(current: SirState): SirState {
        val pop = current.population
        val infectedRange = current.susceptible..(current.susceptible + current.infected)
        var newInfected = 0
        for (i in 1..n) {
            val pair = Random.distinctInts(2, 0, pop)
            if ((pair[0] in infectedRange || pair[1] in infectedRange) && (pair[0] < current.susceptible || pair[1] < current.susceptible)
                    && Random.nextDouble() < infectionRate) {
                newInfected++
            }
        }
        val newRecovered = Random.sampleBinomial(current.infected, recoveryRate)
        return SirState(current.susceptible - newInfected, current.infected + newInfected - newRecovered, current.recovered + newRecovered)
    }
}

fun SirModel.runMixer(infected: Int, immune: Int) {
    var state = SirState(pop.count - infected - immune, infected, immune)
    var peakInfected = infected
    println("---")
    for (i in 1..100) {
        println("$i ${state.susceptible} ${state.infected} ${state.recovered}")
        state = evolve(state)
        peakInfected = max(peakInfected, state.infected)
    }
    println("101 ${state.susceptible} ${state.infected} ${state.recovered}")
    println("---")
    println("Peak Infections: $peakInfected")
}

//endregion

fun main() {
//    SirModel(Population(1000000), SirEvolveOde(.00005, 30.0, 0.02)).runOde(infected = 5, immune = 0)
//    SirModel(Population(1000000), SirMixALot(4000000, 0.04, 0.08)).runMixer(infected = 5, immune = 0)
    SirModel(Population(1000000), SirMixALot2(4000000, 0.04, 0.08)).runMixer(infected = 5, immune = 0)
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)