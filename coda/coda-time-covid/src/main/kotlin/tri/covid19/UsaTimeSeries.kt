/*-
 * #%L
 * coda-data-0.1.23
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
package tri.covid19

import tri.area.USA
import tri.area.usa.*
import tri.area.usa.Usa.censusDivisionOf
import tri.area.usa.Usa.censusRegionOf
import tri.area.usa.Usa.femaRegionOf
import tri.area.usa.Usa.xyRegionOf
import tri.timeseries.TimeSeries
import tri.timeseries.sum
import tri.util.measureTime

// this class contains utilities for processing timeseries data for [Usa] area types

val List<TimeSeries>.counties
    get() = filter { UsaAreaLookup.areaOrNull(it.areaId) is UsCountyInfo }
val List<TimeSeries>.cbsas
    get() = filter { UsaAreaLookup.areaOrNull(it.areaId) is UsCbsaInfo }
val List<TimeSeries>.states
    get() = filter { UsaAreaLookup.areaOrNull(it.areaId) is UsStateInfo }
val List<TimeSeries>.national
    get() = filter { UsaAreaLookup.areaOrNull(it.areaId) == USA }

/**
 * Adds rollups of series to a list of time series. Does not check that the input data is at the proper level.
 * If cumulative, fills missing future values with the last value. Otherwise assumes those values are zero.
 */
fun List<TimeSeries>.withAggregate(cbsa: Boolean = false, state: Boolean = false, regional: Boolean = false, censusRegional: Boolean = false, national: Boolean = false): List<TimeSeries> {
    val res = mutableListOf(this)
    if (cbsa) measureTime { res += aggregateByCbsa().flatMap { it.value } }.also { println("  aggregated $size records to CBSA in $it") }
    if (state) measureTime { res += aggregateByState().flatMap { it.value } }.also { println("  aggregated $size records to State in $it") }
    if (regional) measureTime { res += aggregateByRegion().flatMap { it.value } }.also { println("  aggregated $size records to Region in $it") }
    if (censusRegional) measureTime {
        res += aggregateByCensusRegion().flatMap { it.value }
        res += aggregateByCensusDivision().flatMap { it.value }
        res += aggregateByRegionXY().flatMap { it.value }
    }.also { println("  aggregated $size records to Census Regional in $it") }
    if (national) measureTime { res += aggregateToNational() }.also { println("  aggregated $size records to National in $it") }
    return res.flatten()
}

/** Sums metric data associated with counties and aggregates to CBSA by summing. Assumes time series are US county info. */
fun List<TimeSeries>.aggregateByCbsa(): Map<Int, List<TimeSeries>> {
    return groupBy { listOf(it.source, (UsaAreaLookup.area(it.areaId) as? UsCountyInfo)?.cbsa, it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsCbsaInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { UsaAreaLookup.area(it.areaId).fips!! }
}

/** Sums metric data associated with counties and aggregates to state by summing. Assumes time series are US county info. */
fun List<TimeSeries>.aggregateByState(): Map<String, List<TimeSeries>> {
    return groupBy { listOf(it.source, (UsaAreaLookup.area(it.areaId) as? UsCountyInfo)?.state, it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsStateInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { (UsaAreaLookup.area(it.areaId) as UsStateInfo).abbreviation }
}

/** Sums metric data associated with counties and aggregates to region by summing. Assumes time series are US county info. */
fun List<TimeSeries>.aggregateByRegion(): Map<String, List<TimeSeries>> {
    return groupBy { listOf(it.source, femaRegionOf(UsaAreaLookup.area(it.areaId)), it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsRegionInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { (UsaAreaLookup.area(it.areaId) as UsRegionInfo).id }
}

/** Aggregate by region X/Y. */
fun List<TimeSeries>.aggregateByRegionXY(): Map<String, List<TimeSeries>> {
    return filter { xyRegionOf(UsaAreaLookup.area(it.areaId)) != null }.groupBy { listOf(it.source, xyRegionOf(UsaAreaLookup.area(it.areaId)), it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsRegionInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { (UsaAreaLookup.area(it.areaId) as UsRegionInfo).id }
}

/** Sums metric data associated with counties and aggregates to region by summing. Assumes time series are US county info. */
fun List<TimeSeries>.aggregateByCensusRegion(): Map<String, List<TimeSeries>> {
    return filter { censusRegionOf(UsaAreaLookup.area(it.areaId)) != null }.groupBy { listOf(it.source, censusRegionOf(UsaAreaLookup.area(it.areaId)), it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsRegionInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { (UsaAreaLookup.area(it.areaId) as UsRegionInfo).id }
}

/** Sums metric data associated with counties and aggregates to region by summing. Assumes time series are US county info. */
fun List<TimeSeries>.aggregateByCensusDivision(): Map<String, List<TimeSeries>> {
    return filter { censusDivisionOf(UsaAreaLookup.area(it.areaId)) != null }.groupBy { listOf(it.source, censusDivisionOf(UsaAreaLookup.area(it.areaId)), it.metric, it.qualifier) }.mapValues { data ->
        (data.key[1] as? UsRegionInfo)?.let { data.value.sum(it.id) }
    }.mapNotNull { it.value }.groupBy { (UsaAreaLookup.area(it.areaId) as UsRegionInfo).id }
}

/** Sums metric data and aggregates to USA national. Assumes time series are disjoint areas covering the USA. */
fun List<TimeSeries>.aggregateToNational() = groupBy { listOf(it.source, it.metric, it.qualifier) }
    .map { it.value.sum(USA.id) }
