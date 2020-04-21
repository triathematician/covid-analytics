package triathematician.covid19.ui

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.getProperty
import tornadofx.property
import java.lang.Math.pow

/** Additional config for Hubbert plot. */
class HubbertPlotConfig(var onChange: () -> Unit = {}) {
    var logPeakValue: Number by property(3.0)
    val peakValue: Double
        get() = pow(10.0, logPeakValue.toDouble())
    val showPeakCurve = SimpleBooleanProperty(false).apply { addListener { _ -> onChange() } }

    val logPeakValueProperty = getProperty(HubbertPlotConfig::logPeakValue).apply { addListener { _ -> if (showPeakCurve.get()) onChange() } }
}