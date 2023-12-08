package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Country
import dev.cmartin.microserviceone.Model.ErrorResponse
import dev.cmartin.microserviceone.TestData.countries
import dev.cmartin.microserviceone.TestData.france
import dev.cmartin.microserviceone.TestData.sortByCode
import dev.cmartin.microserviceone.TestData.sortByName
import dev.cmartin.microserviceone.TestData.spain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CountryControllerTest {

    private val countryService = Mockito.mock(CountryService::class.java)

    private val webClient =
        WebTestClient
            .bindToController(CountryController(countryService))
            .controllerAdvice(GlobalExceptionHandler::class.java)
            .build()

    @Test
    fun `should get all countries`() {
        // given
        val expectedCountries = Flux.fromIterable(countries.sortedBy { it.name })
        Mockito
            .`when`(countryService.findAll(sortByName))
            .thenReturn(expectedCountries)

        // when
        val response: EntityExchangeResult<List<Country>> =
            webClient
                .get()
                .uri(BASE_PATH)
                .exchange()
                .expectStatus().isOk
                .expectBodyList(Country::class.java)
                .returnResult()

        // then
        val responseCountries: List<Country> = response.responseBody ?: emptyList()
        assertEquals(responseCountries.size, countries.size)
        assertEquals(france, responseCountries.first())
    }

    @Test
    fun `should get all countries sorted by code`() {
        // given
        val expectedCountries = Flux.fromIterable(countries.sortedBy { it.code })
        Mockito
            .`when`(countryService.findAll(sortByCode))
            .thenReturn(expectedCountries)

        // when
        val response: EntityExchangeResult<List<Country>> =
            webClient
                .get()
                .uri("$BASE_PATH?sortedBy=code")
                .exchange()
                .expectStatus().isOk
                .expectBodyList(Country::class.java)
                .returnResult()

        // then
        val responseCountries: List<Country> = response.responseBody ?: emptyList()
        assertEquals(responseCountries.size, countries.size)
        assertEquals(spain, responseCountries.first())
    }

    @Test
    fun `should get single country by name`() {
        // given
        Mockito
            .`when`(countryService.findByName(spain.name))
            .thenReturn(Mono.just(spain))

        // when
        webClient
            .get()
            .uri("$BASE_PATH?name=${spain.name}")
            .exchange()
            // then
            .expectStatus().isOk
            .expectBodyList(Country::class.java)
            .hasSize(1)
            .contains(spain)
    }

    @Test
    fun `should get single country by code`() {
        // given
        Mockito
            .`when`(countryService.findByCode(spain.code))
            .thenReturn(Mono.just(spain))

        // when
        webClient
            .get()
            .uri("$BASE_PATH/${spain.code}")
            .exchange()
            // then
            .expectStatus().isOk
            .expectBodyList(Country::class.java)
            .hasSize(1)
            .contains(spain)
    }

    @Test
    fun `should return not found for missing country by code`() {
        // given
        Mockito
            .`when`(countryService.findByCode(MISSING_CODE))
            .thenReturn(Mono.empty())

        // when
        val errorResponse = webClient.get()
            .uri("$BASE_PATH/$MISSING_CODE")
            .exchange()
            .expectStatus().isNotFound
            .expectBody(ErrorResponse::class.java)
            .returnResult()
            .responseBody!!

        // then
        assertEquals("$MISSING_CODE not found", errorResponse.message)
    }

    companion object {
        const val BASE_PATH = "/ms-one/countries"
        const val MISSING_CODE = "missing-code"
    }

}
