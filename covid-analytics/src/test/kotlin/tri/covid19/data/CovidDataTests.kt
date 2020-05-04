package tri.covid19.data

import tri.covid19.CASES
import tri.covid19.DEATHS
import tri.timeseries.deltas
import tri.timeseries.movingAverage
import kotlin.math.pow
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    val data = CovidHistory.allData.first { it.region.id == "United Kingdom" && it.metric == CASES }
    println(data.values)
    println(data.values.deltas().movingAverage(7))
    val kernels = data.values.deltas().movingAverage(7).windowed(6).map { bestKernel(it) }
    println(kernels)
    val kernels2 = kernels.windowed(4).map { kernelChangeRule(it) }
    println(kernels2)
}

/** 0/1 are before, 2 is at, 3 is after */
fun kernelChangeRule(priors: List<Kernel>): Kernel {
    val dirs = priors.map { it.dir }
    val dirSet = dirs.toSet()
    val dirOthers = setOf(dirs[0], dirs[1], dirs[3])
    return when {
        priors[2] == Kernel.RAPID_RISE -> priors[2]
        priors[2] == Kernel.RAPID_FALL -> priors[2]
        priors[2].dir == priors[1].dir && priors[2].dir == priors[0].dir -> priors[2]
        priors[0].dir == priors[1].dir && priors[0].dir == priors[1].dir -> priors[1]
        else -> priors[0]
    }

}

fun bestKernel(vals: List<Double>): Kernel {
    val vals2 = (1 until vals.size).map { (vals[it]-vals[it-1])/(.5*vals[it]+.5*vals[it-1]) }
//    println(vals2)
    val rates = vals2.map { rate(it) }
//    println(rates)
    val mid = rates.average() + .1*rates[2]
//    println(mid)
    return when {
        mid >= 1 -> Kernel.RAPID_RISE
        mid >= .2 -> Kernel.RISE
        mid >= -.2 -> Kernel.PLATEAU
        mid >= -1 -> Kernel.FALL
        else -> Kernel.RAPID_FALL
    }
}

fun rate(d: Double) = when {
    d >= 1.0 -> 3
    d >= 0.104 -> 2
    d >= 0.0507 -> 1
    d <= -0.104 -> -2
    d <= -0.0507 -> -1
    else -> 0
}

enum class Kernel(val dir: Int) {
    RAPID_RISE(1),
    RISE(1),
    PLATEAU(0),
    FALL(-1),
    RAPID_FALL(-1)
}