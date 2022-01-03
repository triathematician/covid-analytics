/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
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
import javafx.scene.control.*
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
