package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.CountryService.Companion.SortableProperties
import dev.cmartin.microserviceone.Model.Country
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentMap

@Service
class JsonCountryService(private val countryMap: ConcurrentMap<String, Country>) : CountryService {

    override fun findAll(sortByProperty: SortableProperties, limit: Int): Flux<Country> {
        val countries = Flux.fromIterable(
            countryMap
                .values
                .take(limit)
        ).also { logger.debug("retrieving all countries. size: ${countryMap.size}") }

        return when (sortByProperty) {
            SortableProperties.CODE -> countries.sort(codeComparator)
            SortableProperties.NAME -> countries.sort(nameComparator)
        }
    }

    override fun findByCode(code: String): Mono<Country> =
        this.countryMap[code]
            .toMono()

    override fun findByName(name: String): Mono<Country> =
        Mono.justOrEmpty(
            this.countryMap
                .values
                .find { it.name == name }
        )


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JsonCountryService::class.java)

        private val codeComparator = compareBy(Country::code)
        private val nameComparator = compareBy(Country::name)
    }
}
