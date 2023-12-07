package dev.cmartin.microserviceone

import java.time.Instant

object Model {
    @JvmInline
    value class Code(val value: String)

    @JvmInline
    value class Name(val value: String)

    data class Country(val code: String, val name: String)

    data class CountryNotFoundException(override val message: String) : RuntimeException(message)

    data class ErrorResponse(val message: String, val instant: Instant)
}