package dev.cmartin.microserviceone

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.time.Instant

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Model.CountryNotFoundException::class)
    fun handleCountryNotFoundException(ex: Model.CountryNotFoundException): ResponseEntity<Model.ErrorResponse> {
        logger.info("resource not found: ${ex.message}")
        val errorResponse = Model.ErrorResponse(ex.message, Instant.now())

        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(ex: HandlerMethodValidationException): ResponseEntity<Model.ErrorResponse> {

        val errorMessage = ex.valueResults
            .flatMap { res ->
                res.resolvableErrors
                    .map { e -> "${res.argument}: ${e.defaultMessage}" }
            }
            .joinToString(", ")

        return ResponseEntity(
            Model.ErrorResponse(errorMessage, Instant.now()),
            HttpStatus.BAD_REQUEST
        )
    }


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CountryController::class.java)

    }
}