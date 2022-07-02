/*-
 * #%L
 * coda-data-0.4.0-SNAPSHOT
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
package tri.util.csv

import tri.util.javaTrim
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.net.URL
import java.nio.charset.Charset

/** Split lines of the CSV file, without quotes. */
object CsvLineSplitterFast {
    /** Reads data from the given URL, returning the header line and content lines. */
    fun readData(url: URL, charset: Charset = Charsets.UTF_8) = readData { InputStreamReader(url.openStream(), charset) }

    /** Reads data from the given string, returning the header line and content lines. */
    fun readData(string: String) = readData { StringReader(string) }

    /** Reads data from a reader, returning the header line and content lines. */
    fun readData(reader: () -> Reader) = splitLine(reader().firstLine()) to BufferedReader(reader()).lineSequence().drop(1).map { splitLine(it) }

    /** Splits a comma-separated lines. An empty line will generate an exception. */
    fun splitLine(line: String) = line.split(",").map { it.javaTrim() }
}
