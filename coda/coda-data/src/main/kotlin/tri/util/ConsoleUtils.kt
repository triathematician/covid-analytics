package tri.util

import java.io.File
import java.io.PrintStream
import java.util.logging.Logger

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

/** Prints info log. */
inline fun <reified X> info(message: String) = logger<X>().info(message)
/** Prints warning log. */
inline fun <reified X> warning(message: String) = logger<X>().warning(message)

/** Gets logger for class. */
inline fun <reified X> logger() = Logger.getLogger(X::class.java.name)