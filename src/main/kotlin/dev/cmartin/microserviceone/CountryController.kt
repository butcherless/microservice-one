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

    /**
     * Gets a list of countries.
     *
     * @param sortedBy the property to sort the countries by (default is "name")
     * @param name the name of the country to filter by (default is "")
     * @return a Flux of Country objects
     */
    @GetMapping("", "/")
    fun getCountries(
        @RequestParam(defaultValue = "name") sortedBy: String,
        @RequestParam(defaultValue = "") name: String
    ): Flux<Country> =
        when {
            name.isNotEmpty() -> getByName(name)
            else -> getAllSorted(sortedBy)
        }

    /*
     * Retrieves a Flux of Country objects by name.
     *
     * @param name the name of the country to filter by
     * @return a Flux of Country objects
     */
    private fun getByName(name: String): Flux<Country> =
        Flux.from(
            this.countryService
                .findByName(name)
                .handleMissingCountryError(name)
                .also { logger.debug("$GET_BY_NAME: $name") }
        )

    /*
     * Retrieves a Flux of all countries sorted by the specified property.
     *
     * @param sortedBy the property to sort the countries by (default is "name")
     * @return a Flux of Country objects
     */
    private fun getAllSorted(sortedBy: String): Flux<Country> =
        this.countryService.findAll(resolveSortableProperty(sortedBy))
            .also { logger.debug("$GET_SORTED_BY: $sortedBy") }


    /**
     * Retrieves a Flux of Country objects by code.
     *
     * @param code the code of the country to filter by
     * @return a Flux of Country objects
     */
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

        /*
         * Handles the error when a Country is missing for a given identifier.
         *
         * @param identifier the identifier of the missing Country
         * @return a Mono emitting the error CountryNotFoundException
         * @throws CountryNotFoundException when the Country is not found for the given identifier
         */
        private fun Mono<Country>.handleMissingCountryError(identifier: String) =
            this.switchIfEmpty(Mono.error(CountryNotFoundException("$identifier not found")))

        /*
         * Resolves the sortable property based on the given string.
         *
         * @param sortedBy the property to sort the countries by
         * @return the resolved SortableProperties object
         */
        private fun resolveSortableProperty(sortedBy: String): SortableProperties =
            when (sortedBy.uppercase()) {
                SortableProperties.CODE.name -> SortableProperties.CODE
                else -> SortableProperties.NAME
            }
    }
}
