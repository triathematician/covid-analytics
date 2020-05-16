package tri.covid19.forecaster.utils

import javafx.scene.control.TableColumn
import tornadofx.cellFormat
import tri.util.userFormat
import java.text.NumberFormat

/** Quickly formats column as a "pretty" number. */
fun <S> TableColumn<S, out Number?>.cellFormatUserNumber() = cellFormat { text = it?.userFormat() }
/** Quickly formats column as a percentage. */
fun <S> TableColumn<S, out Number?>.cellFormatPercentage() = cellFormat { text = NumberFormat.getPercentInstance().format(it) }