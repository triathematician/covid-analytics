package triathematician.util

import java.text.NumberFormat

/** Format number with given number of digits. */
internal fun Number.format(digits: Int) = "%.${digits}f".format(this)

/** Format a number with digits for presenting to user. */
internal fun Number.userFormat(): String {
    val x = toDouble()
    return when {
        x >= 10.0 -> toInt().toString()
        x >= 1.0 -> format(1)
        x < 0.01 -> format(3)
        else -> format(2)
    }
}

fun String.javaTrim() = trim { it <= ' ' }

/** Formats integers using given range of digits. */
fun numberFormat(integerDigitRange: IntRange) = NumberFormat.getInstance().apply {
    minimumIntegerDigits = integerDigitRange.first
    maximumFractionDigits = integerDigitRange.last
}