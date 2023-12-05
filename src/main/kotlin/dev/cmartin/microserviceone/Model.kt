package dev.cmartin.microserviceone

object Model {
    @JvmInline
    value class Code(val value: String)

    @JvmInline
    value class Name(val value: String)

    data class Country(val code: String, val name: String)
}