package tri.timeseries.io

import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class TimeSeriesFileFormatTest {

    @Test
    fun testJson() {
        val t = tri.timeseries.TimeSeries("test", "IA", "test", "subpop", 0, LocalDate.now(), listOf(3, 1, 4, 1, 5))
        TimeSeriesFileFormat.writeSeries(t, System.out)
        val t2 = TimeSeriesFileFormat.readSeries(TimeSeriesFileFormat.writeSeriesAsString(t))
        TimeSeriesFileFormat.writeSeries(t2, System.out)
    }

    @Test
    fun testNormalize() {
        println(File("").absoluteFile)
        println(File("../data").absoluteFile)
        println(File("../data").exists())
        println(File("../../data").absoluteFile)
        println(File("../../data").exists())
        println(File("../../../data").absoluteFile)
        println(File("../../../data").exists())
        println(File("../../../../data").absoluteFile)
        println(File("../../../../data").exists())
        println(tri.covid19.data.LocalCovidData.dataDir)
        println(tri.covid19.data.LocalCovidData.jhuCsseProcessedData)

        val proc0 = object : tri.timeseries.TimeSeriesFileProcessor({ tri.covid19.data.JhuDailyReports.rawSources().subList(0, 2) }, { File("test3.csv") }) {
            override fun metricsProvided() = setOf(tri.covid19.CASES, tri.covid19.DEATHS).map { tri.timeseries.MetricInfo(it) }.toSet()
            override fun inprocess(file: File) = tri.covid19.data.JhuDailyReports.inprocess(file)
        }
        measureTime {
            println(proc0.data().size)
        }.also { println(it) }
        measureTime {
            println(proc0.data().size)
        }.also { println(it) }
    }

}