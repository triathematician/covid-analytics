/*-
 * #%L
 * coda-data
 * --
 * Copyright (C) 2020 - 2021 Elisha Peterson
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
package tri.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DefaultMapper: ObjectMapper() {

    init {
        registerModule(KotlinModule())
        registerModule(SimpleModule()
                .addSerializer(LocalDate::class.java, LocalDateSerializer)
                .addDeserializer(LocalDate::class.java, LocalDateDeserializer))
    }

    fun write(x: Any) = writerWithDefaultPrettyPrinter().writeValueAsString(x)

    fun prettyPrint(x: Any) = write(x).also { println(it) }

}

private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

object LocalDateSerializer: JsonSerializer<LocalDate>() {
    override fun serialize(p0: LocalDate, p1: JsonGenerator, p2: SerializerProvider) = p1.writeString(FORMAT.format(p0))
}

object LocalDateDeserializer: JsonDeserializer<LocalDate>() {
    override fun deserialize(p0: JsonParser, p1: DeserializationContext) = p0.readValueAs(String::class.java).toLocalDate(FORMAT)
}
