package tri.timeseries

import tri.util.toLocalDate
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

/** Writes to/from files, with one series per row, similar to a CSV format. */
object TimeSeriesFileFormat {

    /** Reads several series from a file. */
    fun readSeries(file: File) = file.readLines().map { readSeries(it) }

    /** Writes several series to the writer. */
    fun writeSeries(m: List<TimeSeries>, out: OutputStream) = PrintStream(out).use { ps -> m.forEach { writeSeries(it, ps) } }

    /** Writes a single series to the writer. */
    fun writeSeries(m: TimeSeries, out: PrintStream) {
        out.println(writeSeriesAsString(m))
    }

    /** Writes a single series to the writer. */
    fun writeSeriesAsString(m: TimeSeries) =
            (listOf(m.source, m.areaId, m.metric, m.qualifier, m.intSeries, if (m.intSeries) m.defValue.toInt() else m.defValue, m.start)
                    + (if (m.intSeries) m.values.map { it.toInt() } else m.values)).joinToString("\t")

    /** Reads a series from a writer line. */
    fun readSeries(line: String): TimeSeries {
        val split = line.split("\t")
        return TimeSeries(split[0], split[1], split[2], split[3], split[4].toBoolean(), split[5].toDouble(), split[6].toLocalDate(),
                split.subList(7, split.size).map { it.toDouble() })
    }

}