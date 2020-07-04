package tri.util

/** Add n values at the beginning of the list. */
fun <X> List<X>.padLeft(n: Int, value: X) = List(n) { value } + this

/** Check if the string contains any of elements in list. */
infix fun String.containsOneOf(list: List<String>) = list.any { it in this }