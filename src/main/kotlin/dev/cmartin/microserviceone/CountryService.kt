package dev.cmartin.microserviceone

import dev.cmartin.microserviceone.Model.Country
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CountryService {
    fun findAll(): Flux<Country>
    fun findByCode(code:String): Mono<Country>
    fun findByName(name:String): Mono<Country>
}