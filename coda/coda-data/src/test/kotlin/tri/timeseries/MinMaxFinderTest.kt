package tri.timeseries

import org.junit.Test
import tri.area.AreaType
import tri.area.USA
import tri.covid19.data.LocalCovidData
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MinMaxFinderTest {

    @Test
    fun testFind() {
        LocalCovidData.byArea { it.type == AreaType.PROVINCE_STATE && it.parent == USA }
                .filter { " " !in it.metric }
                .onEach {
                    val series = it.deltas().restrictNumberOfStartingZerosTo(1).movingAverage(7)
                    println("${it.areaId} - ${it.metric} - ${series.values.map { it.toInt() }}")
                    println("  " + MinMaxFinder(10).invoke(series))
                }
    }

}