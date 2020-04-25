package triathematician.timeseries

/** Tracks a range with uncertainty lower/upper bounds. */
class UncertaintyRange(val mean: Double, val lower: Double, val upper: Double) {
    constructor(mean: String?, lower: String?, upper: String?) : this(mean!!.toDouble(), lower!!.toDouble(), upper!!.toDouble())
}