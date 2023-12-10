package dev.cmartin.microserviceone

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.cmartin.microserviceone.Model.Country
import org.springframework.core.io.ClassPathResource

object ApplicationUtils {
    /**
     * Reads a JSON file and converts it into a list of [Model.Country] objects.
     *
     * @param path The path to the JSON file.
     * @return A list of [Model.Country] objects.
     */
    fun readJsonFile(path: String): List<Country> =
        jacksonObjectMapper()
            .readValue<List<Country>>(ClassPathResource(path).inputStream)
}