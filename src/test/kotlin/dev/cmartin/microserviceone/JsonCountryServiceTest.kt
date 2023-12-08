package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Country
import dev.cmartin.microserviceone.TestData.countries
import dev.cmartin.microserviceone.TestData.france
import dev.cmartin.microserviceone.TestData.sortByCode
import dev.cmartin.microserviceone.TestData.sortByName
import dev.cmartin.microserviceone.TestData.spain
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class JsonCountryServiceTest {
    private val countryMap: ConcurrentMap<String, Country> =
        ConcurrentHashMap(
            countries.associateBy { it.code }
        )

    private val service: CountryService = JsonCountryService(countryMap)

    @Test
    fun `find all countries sorted by code`() {
        StepVerifier
            .create(service.findAll(sortByCode))
            .expectNext(spain)
            .expectNext(france)
            .verifyComplete()
    }

    @Test
    fun `find all countries sorted by name`() {
        StepVerifier
            .create(service.findAll(sortByName))
            .expectNext(france)
            .expectNext(spain)
            .verifyComplete()
    }

    @Test
    fun `find country by code`() {
        StepVerifier
            .create(service.findByCode(spain.code))
            .expectNext(spain)
            .verifyComplete()
    }

    @Test
    fun `find country by name`() {
        StepVerifier
            .create(service.findByName(spain.name))
            .expectNext(spain)
            .verifyComplete()
    }

    @Test
    fun `find country by name missing entity`() {
        StepVerifier
            .create(service.findByName("missing"))
            .verifyComplete()
    }

}