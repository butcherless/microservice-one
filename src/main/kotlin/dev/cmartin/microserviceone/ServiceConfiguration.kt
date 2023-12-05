package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Country
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Configuration
@ConfigurationProperties(prefix = "service")
class ServiceConfiguration {

    @Value("\${service.countries.file}")
    lateinit var countriesFile: String

    @Bean
    fun countryMap(): ConcurrentMap<String, Country> =
        ConcurrentHashMap(
            ApplicationUtils
                .readJsonFile(countriesFile)
                .associateBy { it.code }
        )
}