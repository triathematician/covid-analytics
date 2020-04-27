package tri.covid19.forecaster

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.getProperty
import tornadofx.property
import tri.util.userFormat
import java.lang.Math.pow

/** Additional config for Hubbert plot. */
class HistoricalHubbertPlots(var onChange: () -> Unit = {}) {
    var logPeakValue: Number by property(3.0)
    val peakValue: Double
        get() = pow(10.0, logPeakValue.toDouble())
    val peakLabel: String
        get() {
            val peak = peakValue
            return "Peak at ${peak.userFormat()}"
        }
    val showPeakCurve = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }

    val logPeakValueProperty = getProperty(HistoricalHubbertPlots::logPeakValue).apply { addListener { _ -> if (showPeakCurve.get()) onChange() } }
}