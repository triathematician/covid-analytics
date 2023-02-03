/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2023 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.covid19.coda.utils

import javafx.event.EventHandler
import javafx.geometry.Side
import javafx.scene.control.ContextMenu
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.util.*


/** Text field that autocompletes. */
class AutocompleteTextField(var options: SortedSet<String> = sortedSetOf()): TextField() {

    private val autocompleteOptions = ContextMenu()

    init {
        focusedProperty().addListener { _, _, _ -> autocompleteOptions.hide() }
        textProperty().addListener { _, _, _ ->
            val enteredText = text
            var filtered = if (text.isNullOrEmpty()) listOf() else options.filter { it.toLowerCase().contains(enteredText.toLowerCase()) }
            when {
                filtered.isEmpty() -> autocompleteOptions.hide()
                else -> {
                    filtered = filtered.filter { it.toLowerCase().startsWith(enteredText.toLowerCase()) } +
                            filtered.filter { !it.toLowerCase().startsWith(enteredText.toLowerCase()) }
                    updatePopup(filtered, enteredText)
                    if (!autocompleteOptions.isShowing && this@AutocompleteTextField.scene != null) {
                        autocompleteOptions.show(this@AutocompleteTextField, Side.BOTTOM, 0.0, 0.0)
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
                            this@AutocompleteTextField.text = it.first
                            positionCaret(it.first.length)
                            autocompleteOptions.hide()
                        }
                    }
                }

        autocompleteOptions.items.setAll(menuItems)
    }
}

internal fun highlightLabel(label: String, enteredText: String) = Label().apply {
    graphic = buildTextFlow(label, enteredText)
    prefHeight = 10.0
}

internal fun buildTextFlow(text: String, filter: String): TextFlow {
    val filterIndex = text.toLowerCase().indexOf(filter.toLowerCase())
    val textFilter = Text(text.substring(filterIndex, filterIndex + filter.length))
    textFilter.fill = Color.ORANGE
    textFilter.font = Font.font("Helvetica", FontWeight.BOLD, 12.0)
    return TextFlow(Text(text.substring(0, filterIndex)), textFilter, Text(text.substring(filterIndex + filter.length)))
}
