package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Code
import dev.cmartin.microserviceone.Model.Country
import dev.cmartin.microserviceone.Model.Name
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux

@RestController
@RequestMapping("ms-one")
class CountryController {

    @GetMapping(countries)
    fun getAllCountries(): Flux<Country> {
        logger.debug("retrieving all countries")
        return getCountries()
            .sortedBy { c -> c.name.value }
            .toFlux()
    }

    companion object {
        val logger = LoggerFactory.getLogger(Companion::class.java.name)
        const val countries = "countries"
        val esCode = Code("es")
        val esName = Name("Spain")
        val ptCode = Code("pt")
        val ptName = Name("Portugal")
        val itCode = Code("it")
        val itName = Name("Italy")
        val frCode = Code("fr")
        val frName = Name("France")
        val deCode = Code("de")
        val deName = Name("Germany")

        val esCountry = Country(esCode, esName)
        val ptCountry = Country(ptCode, ptName)
        val itCountry = Country(itCode, itName)
        val frCountry = Country(frCode, frName)
        val deCountry = Country(deCode, deName)

        fun getCountries(): List<Country> =
            listOf(
                esCountry,
                ptCountry,
                itCountry,
                frCountry,
                deCountry
            )
    }
}
