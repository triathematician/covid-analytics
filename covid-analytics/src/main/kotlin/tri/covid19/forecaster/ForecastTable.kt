package tri.covid19.forecaster

import javafx.scene.control.TableView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import tornadofx.*
import tri.util.DefaultMapper
import tri.util.format
import tri.util.logCsv
import tri.util.userFormat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.time.ExperimentalTime


/** Panel for managing saved forecasts. */
@ExperimentalTime
class ForecastTable(model: ForecastPanelModel) : BorderPane() {

    lateinit var table: TableView<ForecastStats>

    init {
        top = toolbar {
            button("Copy") { action { copyTableDataToClipboard(table) } }
            button("Export...") { action { exportForecastData(model.forecastInfoList) } }
        }
        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            tableview(model.forecastInfoList) {
                readonlyColumn("Region", ForecastStats::regionId)
                readonlyColumn("Model", ForecastStats::model)
                readonlyColumn("Metric", ForecastStats::metric)
                readonlyColumn("Forecast Date", ForecastStats::forecastDate)

                readonlyColumn("Peak Day", ForecastStats::peakDay)
                readonlyColumn("Peak Value", ForecastStats::peakValue).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of April", ForecastStats::apr30Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of May", ForecastStats::may31Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of June", ForecastStats::june30Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of July", ForecastStats::july31Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("May Total", ForecastStats::mayTotal).cellFormat { text = it?.userFormat() }
                readonlyColumn("June Total", ForecastStats::juneTotal).cellFormat { text = it?.userFormat() }
                readonlyColumn("July Total", ForecastStats::julyTotal).cellFormat { text = it?.userFormat() }
                readonlyColumn("Total", ForecastStats::totalValue).cellFormat { text = it?.userFormat() }

                readonlyColumn("Curve", ForecastStats::sigmoidCurve)
                readonlyColumn("Parameters", ForecastStats::parameters).cellFormat { text = it?.joinToString("; ") { it.userFormat() } }
                readonlyColumn("k", ForecastStats::parameterK).cellFormat { text = it?.format(4) }

                readonlyColumn("First Fit Day", ForecastStats::fitFirstDay)
                readonlyColumn("Last Fit Day", ForecastStats::fitLastDay)
                readonlyColumn("RMSE Totals", ForecastStats::rmsErrorCumulative).cellFormat { text = it?.format(2) }
                readonlyColumn("RMSE Deltas", ForecastStats::rmsErrorDelta).cellFormat { text = it?.format(2) }
                readonlyColumn("MASE Totals", ForecastStats::masErrorCumulative).cellFormat { text = it?.format(2) }
                readonlyColumn("MASE Deltas", ForecastStats::masErrorDelta).cellFormat { text = it?.format(2) }

                contextmenu {
                    item("Restore").action { selectedItem?.apply { model.load(this) } }
                    separator()
                    item("Remove").action { selectedItem?.apply { model.forecastInfoList.remove(this) } }
                }

                table = this
            }
        }
    }

    fun <X> copyTableDataToClipboard(table: TableView<X>) {
        val stream = ByteArrayOutputStream()
        val printer = PrintStream(stream)
        table.columns.map { it.text }.logCsv(printer)
        table.items.forEach { row ->
            table.columns.map { it.getCellData(row).forPrinting() }.logCsv(printer)
        }

        val string = stream.toString(Charset.defaultCharset())
        val clipboardContent = ClipboardContent().apply { putString(string) }
        Clipboard.getSystemClipboard().setContent(clipboardContent)

        println(string)
    }

    private fun Any?.forPrinting() = when (this) {
        is DoubleArray -> toList().joinToString("; ")
        is Array<*> -> listOf(*this).joinToString("; ")
        else -> toString()
    }

    private fun exportForecastData(forecastStats: List<ForecastStats>) {
        val fileChooser = FileChooser()
        fileChooser.title = "Export forecast data"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("JSON Files (.json)", "*.json"))
        fileChooser.showSaveDialog(scene.window)?.run {
            DefaultMapper.writerWithDefaultPrettyPrinter().writeValue(this, forecastStats)
        }
    }

}