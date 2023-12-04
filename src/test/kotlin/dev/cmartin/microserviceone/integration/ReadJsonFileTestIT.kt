package dev.cmartin.microserviceone.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource

@SpringBootTest
class ReadJsonFileTestIT {

    data class Country(val code: String, val name: String)

    fun readJsonFile(path: String): Map<String, String> {
        val countries: List<Country> =
            jacksonObjectMapper()
                .readValue(ClassPathResource(path).inputStream)

        return countries.associate { it.code to it.name }
    }

    @Test
    fun `read all countries`() {
        val path = "countries.json"
        val es = Country("es", "Spain")
        val pt = Country("pt", "Portugal")
        val expectedList = mapOf(es.code to es.name, pt.code to pt.name)
        val result = readJsonFile(path)

        Assertions.assertEquals(expectedList, result)
    }
}