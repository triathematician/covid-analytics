package tri.util

import java.time.Duration
import java.time.LocalDateTime

/** Utility to lazy-load using a monitored load function. */
fun <T> lazyMonitor(message: String, initializer: () -> T) = lazy {
    val t0 = LocalDateTime.now()
    println("Loading $message...")
    val res = initializer()
    val duration = Duration.between(t0, LocalDateTime.now())
    println("Loaded $message in ${duration.toSeconds()} seconds")
    res
}

/** Looks up resource by path relative to given class. */
inline fun <reified X> resource(path: String) = X::class.java.getResource(path)