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
package tri.covid19

enum class CovidRiskLevel(var level: Int) {
    MINOR(0),
    MARGINAL(1),
    MODERATE(2),
    URGENT(3),
    SEVERE(4),
    CRITICAL(5)
}

/** Estimated risk based on doubling rates. */
fun risk_DoublingTime(days: Double) = when {
    days <= 2 -> CovidRiskLevel.CRITICAL
    days <= 3 -> CovidRiskLevel.SEVERE
    days <= 4 -> CovidRiskLevel.URGENT
    days <= 7 -> CovidRiskLevel.MODERATE
    days <= 14 -> CovidRiskLevel.MARGINAL
    else -> CovidRiskLevel.MINOR
}

/** Estimated risk based on average deaths per day. */
fun risk_DeathsPerDay(dailyAverage: Double) = risk(dailyAverage, 1.0)

/** Estimated risk based on recent per-capita deaths per day. */
fun risk_PerCapitaDeathsPerDay(dailyAverage: Double) = risk(dailyAverage, 0.01)

/** Risk based on exponential levels. */
private fun risk(value: Double, baseLevel: Double) = when {
    value >= 500*baseLevel -> CovidRiskLevel.CRITICAL
    value >= 100*baseLevel -> CovidRiskLevel.SEVERE
    value >= 20*baseLevel -> CovidRiskLevel.URGENT
    value >= 5*baseLevel -> CovidRiskLevel.MODERATE
    value >= 1*baseLevel -> CovidRiskLevel.MARGINAL
    else -> CovidRiskLevel.MINOR
}
