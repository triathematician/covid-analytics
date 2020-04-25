package triathematician.util

import java.io.File
import java.io.PrintStream

/** Logs first line of file to target output. */
fun File.logFirstLine(ps: PrintStream = System.out, prefix: String = "") = useLines { it.first().log(ps, prefix) }

/** Log content to given output. */
fun Any.log(ps: PrintStream = System.out, prefix: String = "") = ps.println(prefix + this)

/** Log a list of items as tab-separated content lines. */
fun List<Any>.log(ps: PrintStream = System.out, prefix: String = "", sep: String = "\t") = map {
    when (it) {
        is Int -> it
        is Number -> it.format(2)
        else -> it
    }.toString()
}.joinToString(sep).log(ps, prefix)
