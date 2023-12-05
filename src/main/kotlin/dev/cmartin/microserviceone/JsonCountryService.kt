package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.ApplicationUtils.readJsonFile
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

    override fun findAll(sortByProperty: SortableProperties): Flux<Country> {
        logger.debug("retrieving all countries. size: ${countryMap.size}")

        val countries = Flux.fromIterable(readJsonFile(jsonFilePath))

        return when (sortByProperty) {
            SortableProperties.CODE -> countries.sort(codeComparator)
            SortableProperties.NAME -> countries.sort(nameComparator)
        }
    }

    override fun findByCode(code: String): Mono<Country> =
        this.countryMap[code]
            .toMono()

    override fun findByName(name: String): Mono<Country> =
        this.countryMap.entries
            .find { it.value.name == name }
            ?.value.toMono()


    companion object {
        val logger: Logger = LoggerFactory.getLogger(JsonCountryService::class.java)
        val jsonFilePath: String = "countries.json"

        val codeComparator: Comparator<Country> = Comparator { a, b ->
            a.code.compareTo(b.code)
        }

        val nameComparator: Comparator<Country> = Comparator { a, b ->
            a.name.compareTo(b.name)
        }
    }
}