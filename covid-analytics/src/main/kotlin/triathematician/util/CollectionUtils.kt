package triathematician.util

/** Add n values at the beginning of the list. */
fun <X> List<X>.padLeft(n: Int, value: X) = List(n) { value } + this