package tri.timeseries

/** Describes how to fill in missing values in a timeseries when they are unknown. */
enum class TimeSeriesFillStrategy {
    /** Replaces missing values with zeros. */
    FILL_WITH_ZEROS,
    /** Replaces missing values with the last known value with an earlier date. */
    FILL_FORWARD,
    /** Replaces missing values with the first known value with a later date. */
    FILL_BACKWARD,
    /** Leaves values blank if possible. May not always be supported. */
    LEAVE_BLANK;

/*-
 * #%L
 * coda-data-0.3.6-SNAPSHOT
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
}
