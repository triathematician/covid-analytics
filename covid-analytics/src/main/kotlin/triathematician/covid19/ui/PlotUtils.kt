package triathematician.covid19.ui

typealias DataPoints = List<Pair<Number, Number>>

data class DataSeries(var id: String, var points: DataPoints)
