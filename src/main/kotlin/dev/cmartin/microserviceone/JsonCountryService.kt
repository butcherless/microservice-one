package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.CountryService.Companion.SortableProperties
import dev.cmartin.microserviceone.Model.Country
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentMap

@Service
class JsonCountryService(private val countryMap: ConcurrentMap<String, Country>) : CountryService {

    override fun findAll(sortByProperty: SortableProperties): Flux<Country> {
        logger.debug("retrieving all countries. size: ${countryMap.size}")

        val countries = countryMap.values.toFlux()

        return when (sortByProperty) {
            SortableProperties.CODE -> countries.sort(codeComparator)
            SortableProperties.NAME -> countries.sort(nameComparator)
        }
    }

    override fun findByCode(code: String): Mono<Country> =
        this.countryMap[code]
            .toMono()

    override fun findByName(name: String): Mono<Country> =
        this.countryMap.values
            .find { it.name == name }?.toMono()
            ?: Mono.empty()


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JsonCountryService::class.java)
        private val jsonFilePath: String = "countries.json"

        val codeComparator = compareBy(Country::code)
        val nameComparator = compareBy(Country::name)
    }
}
