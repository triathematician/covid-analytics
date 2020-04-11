package triathematician.covid19.sources

import triathematician.covid19.sources.CsseCovid19DailyReports

//
// This file links to various data sources providing time series information.
//

fun dailyReports() = CsseCovid19DailyReports.allTimeSeriesData()