package tri.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DefaultMapper: ObjectMapper() {

    init {
        registerModule(SimpleModule()
                .addSerializer(LocalDate::class.java, LocalDateSerializer)
                .addDeserializer(LocalDate::class.java, LocalDateDeserializer))
    }

    fun write(x: Any) = writerWithDefaultPrettyPrinter().writeValueAsString(x)

}

private val FORMAT = DateTimeFormatter.ofPattern("yyyy-M-dd")

object LocalDateSerializer: JsonSerializer<LocalDate>() {
    override fun serialize(p0: LocalDate, p1: JsonGenerator, p2: SerializerProvider) = p1.writeString(FORMAT.format(p0))
}

object LocalDateDeserializer: JsonDeserializer<LocalDate>() {
    override fun deserialize(p0: JsonParser, p1: DeserializationContext) = p0.readValueAs(String::class.java).toLocalDate(FORMAT)
}