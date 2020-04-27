package tri.util

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.random.Random

/** Sample from a binomial distribution, returning the number of "successes". */
private fun Random.sampleBinomial2(n: Int, p: Double): Int {
    val x = nextDouble().toBigDecimal()
    val q = 1 - p
    var i = -1
    var sum = 0.0.toBigDecimal()
    do {
        i++
        sum += binomialCoeff(n, i, p, q)
    } while (i <= n && x > sum)
    return i
}

private fun binomialCoeff(n: Int, i: Int, p: Double, q: Double): BigDecimal = (n choose i).toBigDecimal() * Math.pow(p, i.toDouble()).toBigDecimal() * Math.pow(q, (n - i).toDouble()).toBigDecimal()

private infix fun Int.choose(k: Int): BigInteger = when {
    k == 0 -> 1.toBigInteger()
    k == 1 -> this.toBigInteger()
    k == 2 -> (this * (this - 1) / 2).toBigInteger()
    k > (this - k) -> choose(this - k)
    else -> ((this - k + 1)..this).multiply() / k.factorial()
}

private fun IntRange.multiply(): BigInteger = map { it.toBigInteger() }.reduce { acc, i -> acc * i }

private fun Int.factorial(): BigInteger = when (this) {
    0 -> 1.toBigInteger()
    1 -> 1.toBigInteger()
    2 -> 2.toBigInteger()
    3 -> 6.toBigInteger()
    4 -> 24.toBigInteger()
    5 -> 120.toBigInteger()
    6 -> 720.toBigInteger()
    else -> this.toBigInteger() * (this - 1).factorial()
}