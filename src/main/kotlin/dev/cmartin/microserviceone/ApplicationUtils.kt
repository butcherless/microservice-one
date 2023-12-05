package dev.cmartin.microserviceone

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource

object ApplicationUtils {
    fun readJsonFile(path: String): List<Model.Country> =
        jacksonObjectMapper()
            .readValue<List<Model.Country>>(ClassPathResource(path).inputStream)
}