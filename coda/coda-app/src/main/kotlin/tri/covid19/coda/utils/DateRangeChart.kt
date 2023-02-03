/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
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

import javafx.event.EventTarget
import javafx.scene.chart.NumberAxis
import tornadofx.attachTo

/**
 * Chart whose x-axis represents a range of dates.
 */
class DateRangeChart(_title: String, xTitle: String, yTitle: String, yLog: Boolean = false)
    : LineChartOnFirstSeries(NumberAxis().apply { label = xTitle },
        (if (yLog) LogAxis() else NumberAxis()).apply { label = yTitle }, 3.0) {

    init {
        title = _title
    }

}

/** Creates chart with date domain. */
fun EventTarget.datechart(title: String, xTitle: String, yTitle: String, yLog: Boolean = false,
                          op: DateRangeChart.() -> Unit = {})
    = DateRangeChart(title, xTitle, yTitle, yLog).attachTo(this, op)
