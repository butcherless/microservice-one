package dev.cmartin.microserviceone.integration

import dev.cmartin.microserviceone.ApplicationUtils
import dev.cmartin.microserviceone.Model.Country
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReadJsonFileTestIT {


    private fun readJsonFile(path: String): Map<String, String> {
        val countries: List<Country> =
            ApplicationUtils.readJsonFile(path)

        return countries.associate { it.code to it.name }
    }

    @Test
    fun `read all countries`() {
        val path = "test-countries.json"
        val es = Country("es", "Spain")
        val pt = Country("pt", "Portugal")
        val expectedList = mapOf(es.code to es.name, pt.code to pt.name)
        val result = readJsonFile(path)

        assertEquals(expectedList, result)
    }
}