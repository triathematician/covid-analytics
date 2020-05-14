package tri.covid19.forecaster.utils

import javafx.event.EventHandler
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.util.*


/** Text field that autocompletes. */
class AutocompleteSpinner(var options: SortedSet<String> = sortedSetOf()): Spinner<String>() {

    private val autocompleteOptions = ContextMenu()

    init {
        focusedProperty().addListener { _, _, _ -> autocompleteOptions.hide() }
        editor.textProperty().addListener { _, _, _ ->
            val enteredText = editor.text
            var filtered = if (editor.text.isNullOrEmpty()) listOf() else options.filter { it.toLowerCase().contains(enteredText.toLowerCase()) }
            when {
                filtered.isEmpty() -> autocompleteOptions.hide()
                else -> {
                    filtered = filtered.filter { it.toLowerCase().startsWith(enteredText.toLowerCase()) } +
                            filtered.filter { !it.toLowerCase().startsWith(enteredText.toLowerCase()) }
                    updatePopup(filtered, enteredText)
                    if (!autocompleteOptions.isShowing && this@AutocompleteSpinner.scene != null) {
                        autocompleteOptions.show(this@AutocompleteSpinner, Side.BOTTOM, 0.0, 0.0)
                    }
                }
            }
        }
    }

    private fun updatePopup(searchResult: List<String>, enteredText: String) {
        val menuItems = searchResult.take(10)
                .map { it to highlightLabel(it, enteredText) }
                .map {
                    CustomMenuItem(it.second, true).apply {
                        onAction = EventHandler { _ ->
                            this@AutocompleteSpinner.editor.text = it.first
                            editor.positionCaret(it.first.length)
                            autocompleteOptions.hide()
                        }
                    }
                }

        autocompleteOptions.items.setAll(menuItems)
    }
}