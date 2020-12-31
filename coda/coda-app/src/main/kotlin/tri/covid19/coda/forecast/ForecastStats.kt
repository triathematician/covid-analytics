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
package tri.covid19.coda.forecast

import com.fasterxml.jackson.annotation.JsonIgnore
import tri.math.SigmoidParameters
import tri.timeseries.Forecast
import tri.util.DateRange
import java.time.LocalDate

val APR30 = LocalDate.of(2020, 4, 30)
val MAY1 = LocalDate.of(2020, 5, 1)
val MAY31 = LocalDate.of(2020, 5, 31)
val JUNE1 = LocalDate.of(2020, 6, 1)
val JUNE30 = LocalDate.of(2020, 6, 20)
val JULY1 = LocalDate.of(2020, 7, 1)
val JULY31 = LocalDate.of(2020, 7, 31)

/** Forecast along with key metrics. */
class ForecastStats(var forecast: Forecast) {

    /** Parameters associated with sigmoid forecast. */
    var sigmoidParameters: SigmoidParameters? = null

    /** Range of dates used for fit. */
    var fitDateRange: DateRange? = null

    /** Root-mean-squared error vs cumulative data. */
    var rmsErrorCumulative: Double? = null
    /** Root-mean-squared  error vs delta data. */
    var rmsErrorDelta: Double? = null

    /** Mean-absolute-scaled error vs cumulative data. */
    var masErrorCumulative: Double? = null
    /** Mean-absolute-scaled error vs delta data. */
    var masErrorDelta: Double? = null

    /** Total forecast value. */
    var totalValue: Number? = null
    /** Day of peak change. */
    var peakDay: LocalDate? = null
    /** Forecasted value at peak. */
    var peakValue: Double? = null

    /** Forecasted value at specific days. */
    var forecastDays = mutableMapOf<LocalDate, Double>()
    /** Forecasted value at specific days. */
    var forecastTotals = mutableMapOf<LocalDate, Double>()

    //region DELEGATE PROPERTIES

    @get:JsonIgnore
    val region
        get() = forecast.area
    @get:JsonIgnore
    val regionId
        get() = forecast.areaId
    @get:JsonIgnore
    val model
        get() = forecast.model
    @get:JsonIgnore
    val metric
        get() = forecast.metric
    @get:JsonIgnore
    val forecastDate
        get() = forecast.forecastDate

    @get:JsonIgnore
    val sigmoidCurve
        get() = sigmoidParameters?.curve
    @get:JsonIgnore
    val parameters
        get() = sigmoidParameters?.parameters
    @get:JsonIgnore
    val parameterK
        get() = sigmoidParameters?.k

    @get:JsonIgnore
    val fitFirstDay
        get() = fitDateRange?.start
    @get:JsonIgnore
    val fitLastDay
        get() = fitDateRange?.endInclusive

    @get:JsonIgnore
    val apr30Total
        get() = forecastTotals[APR30]
    @get:JsonIgnore
    val may31Total
        get() = forecastTotals[MAY31]
    @get:JsonIgnore
    val june30Total
        get() = forecastTotals[JUNE30]
    @get:JsonIgnore
    val july31Total
        get() = forecastTotals[JULY31]
    @get:JsonIgnore
    val mayTotal
        get() = forecastTotals[MAY31]!! - forecastTotals[APR30]!!
    @get:JsonIgnore
    val juneTotal
        get() = forecastTotals[JUNE30]!! - forecastTotals[MAY31]!!
    @get:JsonIgnore
    val julyTotal
        get() = forecastTotals[JULY31]!! - forecastTotals[JUNE30]!!

    //endregion

}
