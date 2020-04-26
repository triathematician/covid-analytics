package triathematician.util

import com.fasterxml.jackson.databind.ObjectMapper

object DefaultMapper: ObjectMapper() {

    init {
//        registerModule(JavaTimeModule())
    }

    fun write(x: Any) = writerWithDefaultPrettyPrinter().writeValueAsString(x)

}