package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Country
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Configuration
@ConfigurationProperties(prefix = "service")
class ServiceConfiguration {
    val logger: Logger = LoggerFactory.getLogger(ServiceConfiguration::class.java)

    @Value("\${service.countries.file}")
    private lateinit var countriesFile: String

    @Bean
    fun countryMap(): ConcurrentMap<String, Country> {
        this.logger.debug("reading countries from file: $countriesFile")

        return ConcurrentHashMap(
            ApplicationUtils
                .readJsonFile(countriesFile)
                .associateBy { it.code }
        )
    }
}