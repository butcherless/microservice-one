package dev.cmartin.microserviceone

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Microservice One")
                    .description("Country lookup service built with Spring Boot and Kotlin.")
                    .version("v1")
                    .contact(Contact().name("Microservice One maintainers"))
            )
}
