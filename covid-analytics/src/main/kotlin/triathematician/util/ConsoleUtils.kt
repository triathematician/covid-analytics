package triathematician.util

import java.io.File

fun File.logFirstLine() = useLines { it.first().log() }
fun Any.log(prefix: String = "") = println(prefix + this)

fun List<Any>.log(prefix: String = "") = map {
    when (it) {
        is Int -> it
        is Number -> it.format(2)
        else -> it
    }.toString()
}.joinToString("\t").log(prefix)

private fun Number.format(digits: Int) = "%.${digits}f".format(this)