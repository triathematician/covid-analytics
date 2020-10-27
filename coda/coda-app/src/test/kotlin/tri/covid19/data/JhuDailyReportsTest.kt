package tri.covid19.data

import org.junit.Test

class JhuDailyReportsTest {

    @Test
    fun testData() {
        println(JhuDailyReports.raw())
        println(JhuDailyReports.processed())
        println(JhuDailyReports.data(null))
        println(JhuDailyReports.data(null))
    }

}