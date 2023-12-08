package dev.cmartin.microserviceone

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource

object ApplicationUtils {
    /**
     * Reads a JSON file and converts it into a list of [Model.Country] objects.
     *
     * @param path The path to the JSON file.
     * @return A list of [Model.Country] objects.
     */
    fun readJsonFile(path: String): List<Model.Country> =
        jacksonObjectMapper()
            .readValue<List<Model.Country>>(ClassPathResource(path).inputStream)
}