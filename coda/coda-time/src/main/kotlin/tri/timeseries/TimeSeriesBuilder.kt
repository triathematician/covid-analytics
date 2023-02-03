/*-
 * #%L
 * coda-data-0.2.3-SNAPSHOT
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
package tri.timeseries

import tri.util.DateRange
import tri.util.rangeTo
import java.time.LocalDate

//region TIME SERIES BUILDERS

/** Helper class for constructing a time series object with minimal boilerplate. */
class TimeSeriesBuilder(val metric: String, val qual: String, val value: Int?) {
    constructor(metric: String, value: Int?): this(metric, "", value)

    fun timeSeries(source: String, areaId: String, date: LocalDate) = TimeSeries(source, areaId, metric, qual,  0, date, value!!)

    /** Generate a time series, where the value is filled forward a given number of days. */
    fun timeSeriesFillForward(source: String, areaId: String, date: LocalDate, forwardFillDays: Int = 0)
            = TimeSeries(source, areaId, metric, qual, 0, date, *MutableList(forwardFillDays + 1) { value!! }.toIntArray())
}

/** Helper class for constructing a time series object with minimal boilerplate. */
class TimeSeriesBuilderDouble(val metric: String, val qual: String, val value: Double?) {
    constructor(metric: String, value: Double?): this(metric, "", value)

    fun timeSeries(source: String, areaId: String, date: LocalDate) = TimeSeries(source, areaId, metric, qual, 0.0, date, value!!)

    /** Generate a time series, where the value is filled forward a given number of days. */
    fun timeSeriesFillForward(source: String, areaId: String, date: LocalDate, forwardFillDays: Int = 0)
            = TimeSeries(source, areaId, metric, qual, 0.0, date, *MutableList(forwardFillDays + 1) { value!! }.toDoubleArray())
}

/** Helper class for constructing time series from dated values. */
class DoubleValuesInTime(map: Map<LocalDate, Double>) {
    val start = map.keys.minOrNull()!!
    val stop = map.keys.maxOrNull()!!
    val values = DateRange(start..stop).map { map.getOrDefault(it, 0.0) }

    /** Construct series from values. */
    fun series(source: String, areaId: String, metric: String, qualifier: String) = TimeSeries(source, areaId, metric, qualifier, false, 0.0, start, values)
}

/** Helper class for constructing time series from dated values. */
class IntValuesInTime(map: Map<LocalDate, Int>) {
    val start = map.keys.minOrNull()!!
    val stop = map.keys.maxOrNull()!!
    val values = DateRange(start..stop).map { map.getOrDefault(it, 0) }

    /** Construct series from values. */
    fun series(source: String, areaId: String, metric: String, qualifier: String) = TimeSeries(source, areaId, metric, qualifier, 0, start, values)
}

//endregion
