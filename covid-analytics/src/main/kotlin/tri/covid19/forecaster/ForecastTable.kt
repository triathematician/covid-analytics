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

    lateinit var table: TableView<UserForecast>

    init {
        top = toolbar {
            button("Copy") { action { copyTableDataToClipboard(table) } }
            button("Export...") { action { exportForecastData(model.userForecasts) } }
        }
        center = scrollpane(fitToWidth = true, fitToHeight = true) {
            tableview(model.userForecasts) {
                readonlyColumn("Region", UserForecast::regionId)
                readonlyColumn("Model", UserForecast::model)
                readonlyColumn("Metric", UserForecast::metric)
                readonlyColumn("Forecast Date", UserForecast::forecastDate)

                readonlyColumn("Peak Day", UserForecast::peakDay)
                readonlyColumn("Peak Value", UserForecast::peakValue).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of April", UserForecast::apr30Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of May", UserForecast::may31Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of June", UserForecast::june30Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("End of July", UserForecast::july31Total).cellFormat { text = it?.userFormat() }
                readonlyColumn("May Total", UserForecast::mayTotal).cellFormat { text = it?.userFormat() }
                readonlyColumn("June Total", UserForecast::juneTotal).cellFormat { text = it?.userFormat() }
                readonlyColumn("July Total", UserForecast::julyTotal).cellFormat { text = it?.userFormat() }
                readonlyColumn("Total", UserForecast::totalValue).cellFormat { text = it?.userFormat() }

                readonlyColumn("Curve", UserForecast::sigmoidCurve)
                readonlyColumn("Parameters", UserForecast::parameters).cellFormat { text = it?.joinToString("; ") { it.userFormat() } }
                readonlyColumn("k", UserForecast::parameterK).cellFormat { text = it?.format(4) }

                readonlyColumn("First Fit Day", UserForecast::fitFirstDay)
                readonlyColumn("Last Fit Day", UserForecast::fitLastDay)
                readonlyColumn("SE Totals", UserForecast::standardErrorCumulative).cellFormat { text = it?.format(2) }
                readonlyColumn("SE Deltas", UserForecast::standardErrorDelta).cellFormat { text = it?.format(2) }

                contextmenu {
                    item("Restore").action { selectedItem?.apply { model.load(this) } }
                    separator()
                    item("Remove").action { selectedItem?.apply { model.userForecasts.remove(this) } }
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

    private fun exportForecastData(forecasts: List<UserForecast>) {
        val fileChooser = FileChooser()
        fileChooser.title = "Export forecast data"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("JSON Files (.json)", "*.json"))
        fileChooser.showSaveDialog(scene.window)?.run {
            DefaultMapper.writerWithDefaultPrettyPrinter().writeValue(this, forecasts)
        }
    }

}