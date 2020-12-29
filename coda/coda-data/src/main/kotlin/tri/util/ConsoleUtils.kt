/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
