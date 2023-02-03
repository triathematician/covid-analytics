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
package tri.covid19.coda

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.chart.XYChart
import javafx.scene.control.Tooltip
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.layout.Priority
import tornadofx.*
import tri.covid19.coda.forecast.ForecastPanel
import tri.covid19.coda.forecast.ForecastTable
import tri.covid19.coda.history.HistoryPanel
import tri.covid19.coda.hotspot.HotspotTable

class CodaApp : App(CodaView::class, CodaStyles::class)

fun main(args: Array<String>) {
    launch<CodaApp>(args)
}

/** View configuration for the app. */
class CodaView : View() {

    override val root = vbox {
        drawer {
            vgrow = Priority.ALWAYS
            item("Historical Data", expanded = true) {
                this += HistoryPanel()
            }
            item("Hotspots") {
                this += HotspotTable()
            }
            val forecastPanel = ForecastPanel()
            item("Forecast Tool") {
                this += forecastPanel
            }
            item("Forecast Table") {
                this += ForecastTable(forecastPanel.model)
            }
        }
        hbox {
            vgrow = Priority.NEVER
            label("")
            pane {
                hgrow = Priority.ALWAYS
            }
            label("")
        }
    }

}

/** Stylesheet for the application. */
class CodaStyles: Stylesheet() {
    companion object {
        val chartHover by cssclass()
    }

    init {
        chartHover {
            effect = DropShadow(BlurType.GAUSSIAN, c("dodgerblue"), 10.0, 0.2, 0.0, 0.0)
        }
    }
}

/** Installs chart hover effect on all data series, adds tooltip with series name. */
fun XYChart<*, *>.installStandardHoverAndTooltip() {
    data.forEach {
        it.installHoverEffect()
        Tooltip.install(it.node, Tooltip(it.name))
    }
}

/** Installs chart hover effect on given node. */
fun Node.installHoverEffect() {
    onMouseEntered = EventHandler { addClass(CodaStyles.chartHover) }
    onMouseExited = EventHandler { removeClass(CodaStyles.chartHover) }
}

/** Installs chart hover effect on given series. */
fun XYChart.Series<*, *>.installHoverEffect() {
    node.installHoverEffect()
    data.forEach { it.node?.let { installHoverEffect() } }
}
