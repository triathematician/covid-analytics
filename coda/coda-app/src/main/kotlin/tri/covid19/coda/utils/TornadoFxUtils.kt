package tri.covid19.coda.utils

import javafx.scene.control.TableColumn
import javafx.scene.paint.Color
import tornadofx.cellFormat
import tri.util.userFormat
import java.text.NumberFormat

/** Quickly formats column as a "pretty" number. */
fun <S> TableColumn<S, out Number?>.cellFormatUserNumber() = cellFormat { text = it?.userFormat() }
/** Quickly formats column as a percentage. */
fun <S> TableColumn<S, out Number?>.cellFormatPercentage() = cellFormat { text = NumberFormat.getPercentInstance().format(it) }
/** Formats column as a trend (days. */
fun <S> TableColumn<S, out Number?>.cellFormatDayTrend() = cellFormat {
    text = when {
        it == null -> ""
        it.toInt() < 0 -> "▼ $it days"
        it.toInt() > 0 -> "▲ $it days"
        else -> "~ $it days"
    }
    textFill = when {
        it == null -> Color.BLACK
        it.toInt() < 0 -> Color.DARKGREEN
        it.toInt() > 0 -> Color.DARKRED
        else -> Color.BLACK
    }
}