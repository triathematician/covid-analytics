package triathematician.covid19.ui

import tornadofx.getProperty
import tornadofx.property
import kotlin.reflect.KMutableProperty1

/** Config for logistic projection. */
class ProjectionPlotConfig(var onChange: () -> Unit = {}) {
    var region by property("US")
    var movingAverage by property(4)
    var predictionDays by property(10)

    //region JAVAFX UI PROPERTIES

    private fun <T> property(prop: KMutableProperty1<*, T>) = getProperty(prop).apply { addListener { _ -> onChange() } }

    val regionProperty = property(ProjectionPlotConfig::region)
    val movingAverageProperty = property(ProjectionPlotConfig::movingAverage)
    val predictionDaysProperty = property(ProjectionPlotConfig::predictionDays)

    //endregion
}