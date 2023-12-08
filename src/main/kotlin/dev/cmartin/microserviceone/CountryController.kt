package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.CountryService.Companion.SortableProperties
import dev.cmartin.microserviceone.Model.Country
import dev.cmartin.microserviceone.Model.CountryNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@RestController
@RequestMapping("ms-one/countries")
class CountryController(private val countryService: CountryService) {

    @GetMapping("", "/")
    fun getCountries(
        @RequestParam(defaultValue = "name") sortedBy: String,
        @RequestParam(defaultValue = "") name: String
    ): Flux<Country> =
        when {
            name.isNotEmpty() -> getByName(name)
            else -> getAllSorted(sortedBy)
        }

    private fun getByName(name: String): Flux<Country> =
        Flux.from(
            this.countryService
                .findByName(name)
                .handleMissingCountryError(name)
                .also { logger.debug("$GET_BY_NAME: $name") }
        )

    private fun getAllSorted(sortedBy: String): Flux<Country> =
        this.countryService.findAll(resolveSortableProperty(sortedBy))
            .also { logger.debug("$GET_SORTED_BY: $sortedBy") }


    @GetMapping("/{code}")
    fun getByCode(@PathVariable(required = true) code: String): Flux<Country> {
        return Flux.from(
            this.countryService
                .findByCode(code)
                .handleMissingCountryError(code)
                .also { logger.debug("$GET_BY_CODE: $code") }
        )
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CountryController::class.java)

        private const val GET_BY_CODE = "get country by code"
        private const val GET_BY_NAME = "get country by name"
        private const val GET_SORTED_BY = "get countries sorted by"

        private fun Mono<Country>.handleMissingCountryError(identifier: String) =
            this.switchIfEmpty(Mono.error(CountryNotFoundException("$identifier not found")))

        private fun resolveSortableProperty(sortedBy: String): SortableProperties =
            when (sortedBy.uppercase()) {
                SortableProperties.CODE.name -> SortableProperties.CODE
                else -> SortableProperties.NAME
            }
    }
}
