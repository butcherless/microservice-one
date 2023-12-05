package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Country
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux

@RestController
@RequestMapping("ms-one")
class CountryController(private val countryService: CountryService) {

    @GetMapping("$countries/")
    fun getAll(): Flux<Country> {
        logger.debug("retrieving all countries")

        return this.countryService
            .findAll()
            .toFlux()
            .sort(codeComparator)
    }

    @GetMapping("$countries/{code}")
    fun getByCode(@PathVariable code: String): ResponseEntity<Country> {
        logger.debug("retrieving country by code: $code")

        val country = this.countryService
            .findByCode(code)
            .block()

        return toOkOrNotFound(country)
    }

    @GetMapping(countries)
    fun getByName(@RequestParam name: String): ResponseEntity<Country> {
        logger.debug("retrieving country by name: $name")
        val country = this.countryService
            .findByName(name)
            .block()

        return toOkOrNotFound(country)
    }

    private fun toOkOrNotFound(country: Country?) =
        country?.let {
            ResponseEntity.ok(country)
        } ?: ResponseEntity
            .notFound()
            .build()
    

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CountryController::class.java)

        const val countries: String = "countries"

        val codeComparator: Comparator<Country> = Comparator { a, b ->
            a.name.compareTo(b.name)
        }

    }
}
