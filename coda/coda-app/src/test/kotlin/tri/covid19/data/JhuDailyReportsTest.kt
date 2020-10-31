package tri.covid19.data

import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class JhuDailyReportsTest {

    @Test
    fun testData() {
        println(JhuDailyReports.rawSources())
        println(JhuDailyReports.processed())
        println(JhuDailyReports.data().size)
        println(JhuDailyReports.data().size)
    }

}