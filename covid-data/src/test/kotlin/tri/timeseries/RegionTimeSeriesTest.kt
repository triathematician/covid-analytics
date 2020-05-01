package tri.timeseries

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Test
import tri.regions.RegionLookup
import tri.util.DefaultMapper
import java.time.LocalDate

class RegionTimeSeriesTest {

    @Test
    fun testJson() {
        val r = RegionTimeSeries(RegionLookup("Iowa"), intTimeSeries("Iowa", "", "test", LocalDate.now(), 5))
        val json = DefaultMapper.prettyPrint(r)

        val rts2 = DefaultMapper.readValue<RegionTimeSeries>(json)
        println(DefaultMapper.prettyPrint(rts2))
    }

}