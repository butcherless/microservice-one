package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Country
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * CountryService is an interface that defines the operations for managing countries.
 */
interface CountryService {
    /**
     * Retrieves a Flux of all countries.
     *
     * @param sortByProperty the property to sort the countries by (default is SortableProperties.NAME)
     * @param limit the maximum number of countries to retrieve (default is 20)
     * @return a Flux of Country objects
     */
    fun findAll(sortByProperty: SortableProperties, limit: Int = 20): Flux<Country>

    /**
     * Retrieves a single Country object by its code.
     *
     * @param code the code of the country to retrieve
     *
     * @return a Mono emitting the Country object
     */
    fun findByCode(code: String): Mono<Country>

    /**
     * Retrieves a Mono of a Country object by its name.
     *
     * @param name the name of the country to search for
     * @return a Mono of Country object
     */
    fun findByName(name: String): Mono<Country>

    companion object {
        enum class SortableProperties {
            CODE,
            NAME
        }
    }
}