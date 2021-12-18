package tri.timeseries.daily

import tri.timeseries.TimeSeries
import java.time.LocalDate
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DailyTimeSeriesTest {

    @Test
    fun testAverage() {
        val values = listOf(1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2, 2, 2)
        val t = TimeSeries("test", "IA", "test", "subpop", 0, LocalDate.now(), values)
    }

}