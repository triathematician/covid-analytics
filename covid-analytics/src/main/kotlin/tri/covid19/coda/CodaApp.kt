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
import kotlin.time.ExperimentalTime

@ExperimentalTime
class CodaApp : App(CodaView::class, CodaStyles::class)

@ExperimentalTime
fun main(args: Array<String>) {
    launch<CodaApp>(args)
}

/** View configuration for the app. */
@ExperimentalTime
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