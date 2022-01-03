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

import javafx.scene.chart.Axis
import javafx.scene.chart.LineChart

/**
 * Chart that updates range based on first series data only.
 * A parameter allows axis range to stretch by no more than the data times that max.
 */
open class LineChartOnFirstSeries(xAxis: Axis<Number>, yAxis: Axis<Number>, val maxMultiplier: Double) : LineChart<Number, Number>(xAxis, yAxis) {

    override fun updateAxisRange() {
        super.updateAxisRange()

        val xa = xAxis
        val ya = yAxis

        val xData = if (xa.isAutoRanging) mutableListOf<Number>() else null
        val yData = if (ya.isAutoRanging) mutableListOf<Number>() else null

        if (xData != null || yData != null) {
            val maxX0 = data.getOrNull(0)?.maxX()
            val maxY0 = data.getOrNull(0)?.maxY()
            val maxX = data.mapNotNull { it.maxX() }.maxOrNull()
            val maxY = data.mapNotNull { it.maxY() }.maxOrNull()
            val xm = if (maxX0 == null || maxX == null) 1.0 else if (maxX >= maxMultiplier * maxX0) maxMultiplier else maxX/maxX0
            val ym = if (maxY0 == null || maxY == null) 1.0 else if (maxY >= maxMultiplier * maxY0) maxMultiplier else maxY/maxY0
            data.firstOrNull()?.let {
                xData?.addAll(it.data.map { it.xValue.toDouble() * xm })
                yData?.addAll(it.data.map { it.yValue.toDouble() * ym })
            }
            if (xData != null && !(xData.size == 1 && xAxis.toNumericValue(xData[0]) == 0.0)) {
                xa.invalidateRange(xData)
            }
            if (yData != null && !(yData.size == 1 && yAxis.toNumericValue(yData[0]) == 0.0)) {
                ya.invalidateRange(yData)
            }
        }
    }

    private fun Series<Number, Number>.maxX() = data.map { it.xValue.toDouble() }.maxOrNull()
    private fun Series<Number, Number>.maxY() = data.map { it.yValue.toDouble() }.maxOrNull()
}
