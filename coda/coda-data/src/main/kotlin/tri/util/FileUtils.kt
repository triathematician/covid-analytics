package tri.util

import java.io.File

val File.url
    get() = toURI().toURL()