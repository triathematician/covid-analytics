/*-
 * #%L
 * coda-utils-0.5.1-SNAPSHOT
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
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

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// Utilities for measuring code execution times.

/** Combined value and time to create value. */
data class TimedValue<T>(val value: T, val duration: Duration)

/** Measures the execution time of a given code block, returning a duration and value. */
inline fun <T> measureTimedValue(block: () -> T): TimedValue<T> {
    val mark = System.nanoTime()
    val result = block()
    val elapsed = (System.nanoTime() - mark).toDuration(DurationUnit.NANOSECONDS)
    return TimedValue(result, elapsed)
}

/** Measures the execution time of a given code block, returning a duration and value. */
inline fun measureTime(block: () -> Unit): Duration {
    val mark = System.nanoTime()
    block()
    return (System.nanoTime() - mark).toDuration(DurationUnit.NANOSECONDS)
}
