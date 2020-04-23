package triathematician.covid19.ui

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.getProperty
import tornadofx.property
import triathematician.util.format
import java.lang.Math.pow

/** Additional config for Hubbert plot. */
class HistoricalHubbertPlots(var onChange: () -> Unit = {}) {
    var logPeakValue: Number by property(3.0)
    val peakValue: Double
        get() = pow(10.0, logPeakValue.toDouble())
    val peakLabel: String
        get() {
            val peak = peakValue
            return "Peak at " + if (peak >= 10.0) peak.toInt() else if (peak >= 1.0) peak.format(1) else peak.format(2)
        }
    val showPeakCurve = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }

    val logPeakValueProperty = getProperty(HistoricalHubbertPlots::logPeakValue).apply { addListener { _ -> if (showPeakCurve.get()) onChange() } }
}