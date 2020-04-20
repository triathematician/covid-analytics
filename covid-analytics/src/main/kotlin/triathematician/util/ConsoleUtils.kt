package triathematician.util

import java.io.File
import java.io.PrintStream

fun File.logFirstLine() = useLines { it.first().log() }
fun Any.log(ps: PrintStream = System.out, prefix: String = "") = ps.println(prefix + this)

fun List<Any>.log(ps: PrintStream = System.out, prefix: String = "", sep: String = "\t") = map {
    when (it) {
        is Int -> it
        is Number -> it.format(2)
        else -> it
    }.toString()
}.joinToString(sep).log(ps, prefix)

fun List<Any>.logCsv(ps: PrintStream = System.out, prefix: String = "", sep: String = ",") = map {
    when (it) {
        is Int -> it
        is Number -> if (it.toDouble() >= 0.1) it.format(3) else it.format(6)
        else -> it
    }.toString()
}.map { if (',' in it) "\"$it\"" else it }
        .joinToString(sep).log(ps, prefix)

internal fun Number.format(digits: Int) = "%.${digits}f".format(this)