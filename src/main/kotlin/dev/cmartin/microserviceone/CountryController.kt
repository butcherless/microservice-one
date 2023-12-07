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
        @RequestParam(defaultValue = "name") orderBy: String,
        @RequestParam(defaultValue = "") name: String
    ): Flux<Country> =
        when {
            name.isNotEmpty() -> getByName(name)
            else -> getAllSorted(orderBy)
        }

    private fun getByName(name: String): Flux<Country> =
        Flux.from(
            this.countryService
                .findByName(name)
                .switchIfEmpty(Mono.error(CountryNotFoundException("$name not found")))
                .also { logger.debug("get country by name: $name") }
        )

    private fun getAllSorted(orderBy: String): Flux<Country> =
        this.countryService.findAll(resolveSortableProperty(orderBy))
            .also { logger.debug("get countries order by: $orderBy") }


    @GetMapping("/{code}")
    fun getByCode(@PathVariable(required = true) code: String): Flux<Country> {
        return Flux.from(
            this.countryService
                .findByCode(code)
                .switchIfEmpty(Mono.error(CountryNotFoundException("$code not found")))
                .also { logger.debug("get country by code: $code") }
        )
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CountryController::class.java)

        private fun resolveSortableProperty(orderBy: String): SortableProperties =
            when (orderBy.uppercase()) {
                SortableProperties.CODE.name -> SortableProperties.CODE
                else -> SortableProperties.NAME
            }
    }
}
