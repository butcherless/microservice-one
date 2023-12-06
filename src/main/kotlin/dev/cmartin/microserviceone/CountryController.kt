package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.CountryService.Companion.SortableProperties
import dev.cmartin.microserviceone.Model.Country
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("ms-one")
class CountryController(private val countryService: CountryService) {

    /**
     * Retrieves all countries by name.
     *
     * @return a Flux of [Country] representing all countries.
     */
    @GetMapping("$countries/")
    fun getAll(): Flux<Country> {
        logger.debug("retrieving all countries by name")

        return this.countryService
            .findAll(SortableProperties.NAME)
    }

    @GetMapping("$countries/{code}")
    fun getByCode(@PathVariable code: String): Mono<ResponseEntity<Country>> {
        logger.debug("retrieving country by code: $code")

        return this.countryService
            .findByCode(code)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @GetMapping(countries)
    fun getByName(@RequestParam name: String): Mono<ResponseEntity<Country>> {
        logger.debug("retrieving country by name: $name")
        return this.countryService
            .findByName(name)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    @GetMapping("$countries/sortedByCode")
    fun getSortedByName(): Flux<Country> {
        logger.debug("retrieving all countries by code")

        return this.countryService
            .findAll(SortableProperties.CODE)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CountryController::class.java)

        const val countries: String = "countries"
    }
}
