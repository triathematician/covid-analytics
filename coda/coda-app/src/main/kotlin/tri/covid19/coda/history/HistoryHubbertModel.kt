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
package tri.covid19.coda.history

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.getProperty
import tornadofx.property
import tri.util.userFormat
import java.lang.Math.pow

/** Additional config for Hubbert plot. */
class HistoryHubbertModel(var onChange: () -> Unit = {}) {
    var logPeakValue: Number by property(3.0)
    val peakValue: Double
        get() = pow(10.0, logPeakValue.toDouble())
    val peakLabel: String
        get() {
            val peak = peakValue
            return "Peak at ${peak.userFormat()}"
        }
    val showPeakCurve = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }

    val logPeakValueProperty = getProperty(HistoryHubbertModel::logPeakValue).apply { addListener { _ -> if (showPeakCurve.get()) onChange() } }
}
