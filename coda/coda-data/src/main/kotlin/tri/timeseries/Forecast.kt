/*-
 * #%L
 * coda-data
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
package tri.timeseries

import tri.area.Lookup
import java.time.LocalDate

/** A single forecast, with model/forecast date, targeted region/metric, and associated time series with forecast data. */
data class Forecast(val model: String, val forecastDate: LocalDate, val areaId: String, val metric: String, val data: List<TimeSeries>) {
    val area = Lookup.areaOrNull(areaId)!!

    constructor(forecastId: ForecastId, data: List<TimeSeries>) : this(forecastId.model, forecastId.forecastDate, forecastId.areaId, forecastId.metric, data)
}

/** Unique tuple describing a forecast. */
data class ForecastId(val model: String, val forecastDate: LocalDate, val areaId: String, val metric: String)
