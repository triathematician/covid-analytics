package tri.util

import kotlin.random.Random

/** Sample from a binomial distribution, returning the number of "successes". */
fun Random.sampleBinomial(n: Int, p: Double) = (1..n).count { nextDouble() < p }

/** Select n distinct integers from the given range. Both range endpoints are inclusive. */
fun Random.distinctInts(n: Int, min: Int, max: Int): IntArray {
    require(max >= min)
    val count = (max - min) + 1
    if (n >= count) throw IllegalArgumentException()
    val chosen = mutableSetOf<Int>()
    for (i in 1..n) {
        var r: Int
        do {
            r = nextInt(min, max + 1)
        } while (r in chosen)
        chosen += r
    }
    return chosen.toIntArray()
}