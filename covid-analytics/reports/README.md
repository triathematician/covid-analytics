# Files
This directory contains three types of files generated from the JHU COVID-19 data available from https://github.com/CSSEGISandData/COVID-19.

## Indicators
The three files with the "indicators" suffix contains data in the form "Region,Metric,Date,Value". The metrics include *confirmed cases*, *deaths*, *recovered*, and *active* counts, each of which indicate the total at that point in time. In addition, there are the derived metrics:
- *per 100k* for per-capita counts
- *growth* for the percentage growth rate from the previous day
- *predicted total* along with *min/max bounds* based on statistical fit to logistic curve
- *predicted peak/days to peak* with daily projections of when the peak might occur

Predictions are made by first smoothing data (averaging over last four days), and then using the last 10 available data points to compute growth rates and fit a logistic growth curve. Historical predictions are provided to help validate this method.

The three files are:
- country_indicators.csv
- us_state_indicators.csv
- us_county_indicators.csv

## Case Hotspots
Files with the "case_hotspots" suffix include ranked lists of recent "hotspots", based on a combination of the per-capita growth for a given day of the "Confirmed Cases" metric and the doubling time (averaging over several days to ensure stability). These files are separated by day and include the following fields:
- location
- metrics
- value
- average of recent changes
- growth rate
- doubling time in days
- severity score based on recent changes (from 0 to 5)
- severity score based on recent growth trajectory (from 0 to 5)
- severity score (total)
- severity score trend (maximum increase or decrease in the last two days)

## Mortality Hotspots
These are the same as the above, just using the death count.