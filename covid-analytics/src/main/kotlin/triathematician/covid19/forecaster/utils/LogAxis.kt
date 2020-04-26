package triathematician.covid19.forecaster.utils

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.chart.ValueAxis
import javafx.util.Duration
import triathematician.util.numberFormat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow


/** Log axis for plotting JavaFX charts. */
class LogAxis : ValueAxis<Number>(1.0, 100.0) {

    private val logUpperBound = SimpleDoubleProperty()
    private val logLowerBound = SimpleDoubleProperty()

    private val ANIMATION_TIME = 2000.0
    private val lowerRangeTimeline = Timeline()
    private val upperRangeTimeline = Timeline()

    init {
        logLowerBound.bindToLogOf(lowerBoundProperty())
        logUpperBound.bindToLogOf(upperBoundProperty())
        autoRangingProperty().set(true)
    }

    /** Validates bounds that support logs. */
    private fun validateBounds(lowerBound: Double, upperBound: Double) {
        check(lowerBound > 0 && upperBound > 0 && upperBound > lowerBound) { "Invalid log range: $lowerBound to $upperBound" }
    }

    //region AXIS METHODS

    override fun getRange() = doubleArrayOf(lowerBound, upperBound)

    override fun autoRange(minValue: Double, maxValue: Double, length: Double, labelSize: Double): DoubleArray {
        val min = largestPowerOfTenBelow(if (minValue <= 0.0) 1.0 else minValue)
        val max = smallestPowerOfTenAbove(maxValue)
        return doubleArrayOf(min, if (min == max) min*10 else max)
    }

    private fun largestPowerOfTenBelow(x: Double) = 10.0.pow(floor(log10(x)))
    private fun smallestPowerOfTenAbove(x: Double) = 10.0.pow(ceil(log10(x)))

    override fun setRange(range: Any?, animate: Boolean) {
        if (range is DoubleArray) {
            validateBounds(range[0], range[1])
            if (animate) {
//                try {
                lowerRangeTimeline.keyFrames.clear()
                upperRangeTimeline.keyFrames.clear()
                lowerRangeTimeline.keyFrames.addAll(KeyFrame(Duration.ZERO, KeyValue(lowerBoundProperty(), lowerBoundProperty().get())),
                        KeyFrame(Duration(ANIMATION_TIME), KeyValue(lowerBoundProperty(), range[0])))
                upperRangeTimeline.keyFrames.addAll(KeyFrame(Duration.ZERO, KeyValue(upperBoundProperty(), upperBoundProperty().get())),
                        KeyFrame(Duration(ANIMATION_TIME), KeyValue(upperBoundProperty(), range[1])))
                lowerRangeTimeline.play()
                upperRangeTimeline.play()
            } else {
                lowerBoundProperty().set(range[0])
                upperBoundProperty().set(range[1])
            }
        }
    }

    override fun getValueForDisplay(displayPosition: Double): Number {
        val delta = logUpperBound.get() - logLowerBound.get()
        return when {
            side.isVertical -> 10.0.pow((displayPosition - height) / -height * delta + logLowerBound.get())
            else -> 10.0.pow(displayPosition / width * delta + logLowerBound.get())
        }
    }

    override fun getDisplayPosition(value: Number): Double {
        val delta = logUpperBound.get() - logLowerBound.get()
        val deltaV = log10(value.toDouble()) - logLowerBound.get()
        return when {
            side.isVertical -> (1.0 - deltaV / delta) * height
            else -> deltaV / delta * width
        }
    }

    override fun calculateTickValues(length: Double, range: Any?) = mutableListOf<Number>().apply {
        if (range is DoubleArray) {
            var i = log10(maxOf(range[0], 1E-5))
            while (i <= log10(range[1])) {
                add(10.0.pow(i))
                i++
            }
            lastOrNull()?.let { if (it != range[1]) add(it) }
        }
    }

    override fun calculateMinorTickMarks() = calculateTickValues(0.0, range)
            .flatMap { m -> (0..minorTickCount).map { m.toDouble() * 10.0.pow(it/minorTickCount.toDouble()) } }

    override fun getTickMarkLabel(value: Number) = numberFormat(integerDigitRange = 1..10).format(value)

}


/** Binds property to log value of another property. */
private fun DoubleProperty.bindToLogOf(base: DoubleProperty) {
    bind(object : DoubleBinding() {
        init { super.bind(base) }
        override fun computeValue() = log10(base.get())
    })
}