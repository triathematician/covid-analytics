/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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
