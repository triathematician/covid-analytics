package triathematician.covid19.sources

import triathematician.covid19.sources.CsseCovid19DailyReports

//
// This file links to various data sources providing time series information.
//

fun dailyReports(idFilter: (String) -> Boolean = { true }) = CsseCovid19DailyReports.allTimeSeriesData()
        .filter { idFilter(it.id) }
        .flatMap {
            listOfNotNull(it, it.scaledByPopulation { "$it (per 100k)" }, it.movingAverage(7).growthPercentages { "$it (growth) " }
            ) + it.movingAverage(7).logisticPredictions(9)
        }