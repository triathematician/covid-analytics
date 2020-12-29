/*-
 * #%L
 * coda-app
 * --
 * Copyright (C) 2020 Elisha Peterson
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
package tri.covid19.data

import tri.timeseries.Forecast
import tri.timeseries.ForecastId
import tri.timeseries.TimeSeriesFileFormat
import java.io.File
import java.time.LocalDate
import kotlin.time.ExperimentalTime

/** Access to forecasts by model and date. */
@ExperimentalTime
object CovidForecasts {

    val FORECAST_OPTIONS = listOf(IHME, LANL, YYG)

    fun modelColor(name: String) = when {
        IHME in name -> "008000"
        LANL in name -> "4682b4"
        YYG in name -> "b44682"
        else -> "808080"
    }

    val allForecasts: List<Forecast> by lazy { loadForecasts() }

    private fun loadForecasts(): List<Forecast> {
        return File("../data/normalized/").walk().filter { it.name.endsWith("-forecasts.json") }.toList()
                .flatMap { fileForecasts(it) }
    }

    private fun fileForecasts(file: File): List<Forecast> {
        val model = file.nameWithoutExtension.substringBefore("-").toUpperCase()
        return TimeSeriesFileFormat.readSeries(file).groupBy {
            // TODO - doesn't handle different forecast dates
            ForecastId(model, LocalDate.now(), it.areaId, it.metric)
        }.map { Forecast(it.key, it.value) }
    }

}

