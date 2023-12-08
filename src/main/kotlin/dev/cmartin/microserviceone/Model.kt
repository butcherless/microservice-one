package dev.cmartin.microserviceone

import java.time.Instant

object Model {
    @JvmInline
    value class Code(val value: String)

    @JvmInline
    value class Name(val value: String)

    /**
     * Represents a country with its code and name.
     *
     * @property code The code of the country.
     * @property name The name of the country.
     */
    data class Country(val code: String, val name: String)

    /**
     * Exception thrown when a country is not found.
     *
     * @property message The error message.
     */
    data class CountryNotFoundException(override val message: String) : RuntimeException(message)

    /**
     * Represents an error response containing a message and the instant it occurred.
     *
     * @property message The error message.
     * @property instant The instant the error occurred.
     */
    data class ErrorResponse(val message: String, val instant: Instant)
}