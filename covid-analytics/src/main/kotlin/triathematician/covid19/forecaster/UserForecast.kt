package triathematician.covid19.forecaster

import com.fasterxml.jackson.annotation.JsonIgnore
import triathematician.math.SigmoidParameters
import triathematician.timeseries.Forecast
import triathematician.util.DateRange
import java.time.LocalDate

val MAY1 = LocalDate.of(2020, 5, 1)
val JUNE1 = LocalDate.of(2020, 6, 1)
val JULY1 = LocalDate.of(2020, 7, 1)

/** Forecast along with key metrics. */
class UserForecast(var forecast: Forecast) {

    /** Parameters associated with sigmoid forecast. */
    var sigmoidParameters: SigmoidParameters? = null

    /** Range of dates used for fit. */
    var fitDayRange: DateRange? = null

    /** Standard error vs cumulative data. */
    var standardErrorCumulative: Double? = null
    /** Standard error vs delta data. */
    var standardErrorDelta: Double? = null

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
        get() = forecast.region
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
        get() = fitDayRange?.start
    @get:JsonIgnore
    val fitLastDay
        get() = fitDayRange?.endInclusive

    @get:JsonIgnore
    val may1
        get() = forecastDays[MAY1]
    @get:JsonIgnore
    val june1
        get() = forecastDays[JUNE1]
    @get:JsonIgnore
    val july1
        get() = forecastDays[JULY1]
    @get:JsonIgnore
    val may1Total
        get() = forecastTotals[MAY1]
    @get:JsonIgnore
    val june1Total
        get() = forecastTotals[JUNE1]
    @get:JsonIgnore
    val july1Total
        get() = forecastTotals[JULY1]

    //endregion

}