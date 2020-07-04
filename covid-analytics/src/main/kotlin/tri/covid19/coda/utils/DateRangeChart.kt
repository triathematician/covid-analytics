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
