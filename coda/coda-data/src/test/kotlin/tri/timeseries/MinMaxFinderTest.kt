package tri.timeseries

import org.junit.Test
import tri.area.AreaType
import tri.covid19.data.LocalCovidData

class MinMaxFinderTest {

    @Test
    fun testFind() {
        LocalCovidData.byArea { it.id.endsWith(", US") && it.type == AreaType.PROVINCE_STATE }
                .onEach {
                    val series = it.deltas().restrictNumberOfStartingZerosTo(1).movingAverage(7)
                    println("${it.areaId} - ${it.metric} - ${series.values.map { it.toInt() }}")
                    println("  " + MinMaxFinder(10).invoke(series))
                }
    }

}