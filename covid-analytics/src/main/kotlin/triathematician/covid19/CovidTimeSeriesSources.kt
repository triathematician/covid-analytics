package triathematician.covid19

//
// This file links to various data sources providing time series information.
//

fun dailyReports() = CsseCovid19DailyReports.allTimeSeriesData()