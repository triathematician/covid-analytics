package tri.timeseries

import org.junit.Test
import tri.covid19.data.JhuDailyReports
import tri.covid19.data.LocalCovidData
import java.io.File
import java.time.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class TimeSeriesFileFormatTest {

    @Test
    fun testJson() {
        val t = TimeSeries("test", "IA", "test", "subpop", 0, LocalDate.now(), listOf(3, 1, 4, 1, 5))
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
        println(LocalCovidData.dataDir)
        println(LocalCovidData.jhuCsseProcessedData)
        val proc0 = object : TimeSeriesFileProcessor({ JhuDailyReports.rawSources() }, { File("test3.csv") }) {
            override fun inprocess(file: File) = JhuDailyReports.inprocess(file)
        }
        measureTime {
            println(proc0.data().size)
        }.also { println(it) }
        measureTime {
            println(proc0.data().size)
        }.also { println(it) }
    }

}