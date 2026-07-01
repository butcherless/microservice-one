# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Microservice One is a country lookup service built with Spring Boot and Kotlin, also used to explore integration with
Spring Cloud Gateway. It reads country data from a bundled JSON resource (`src/main/resources/countries.json`) into
an in-memory map at startup and exposes it over a reactive (WebFlux-style, `Flux`/`Mono`) REST API implemented with
Spring MVC controllers.

Tech stack: Spring Boot 4.1.0, Kotlin 2.4.0, Java 25, Maven 3.9.16 (via wrapper), Spring MVC + WebFlux + Project
Reactor, Spring Security, Bean Validation, Springdoc OpenAPI, JaCoCo.

## Commands

All commands use the Maven Wrapper (`./mvnw`); a separate Maven install is not required.

```bash
./mvnw clean verify          # full build: compiles, runs unit + integration tests, packages jar, generates JaCoCo report
./mvnw clean package         # build the executable jar
java -jar target/microservice-one-0.0.1-SNAPSHOT.jar   # run the packaged jar
./mvnw spring-boot:run       # run directly for development (port 8081, context path /ms-one)
```

Running a single test class:

```bash
./mvnw test -Dtest=CountryControllerTest
./mvnw verify -Dit.test=ReadJsonFileTestIT   # integration tests end in *IT and run via failsafe in the verify phase
```

Force tests to actually run instead of being served from the Maven build cache:

```bash
./mvnw clean verify -Dunit.test.skip=false -Dmaven.build.cache.skipCache=true   # bypass cache reads, still write cache
./mvnw clean verify -Dunit.test.skip=false -Dmaven.build.cache.enabled=false    # disable cache entirely
```

Local build-cache entries live under `~/.m2/build-cache` (max 3 builds retained per artifact); CI persists both
`~/.m2/repository` and `~/.m2/build-cache` between runs, so an unmodified `clean verify` may restore results instead
of re-executing them — pass the flags above when you need a real run.

Other useful checks:

```bash
./mvnw dependency:list -DincludeGroupIds=org.springframework
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates
```

## Architecture

Single Spring Boot module, all production code under `dev.cmartin.microserviceone`:

- `MicroserviceOneApplication.kt` — entry point (`@SpringBootApplication`).
- `ServiceConfiguration.kt` — `@ConfigurationProperties(prefix = "service")`; exposes the `countryMap`
  `ConcurrentMap<String, Country>` bean, built once at startup by reading the JSON file named in
  `service.countries.file` (see `application.yml`) via `ApplicationUtils.readJsonFile`, keyed by country code.
- `ApplicationUtils.kt` — Jackson (kotlin module) helper that deserializes a classpath JSON resource into
  `List<Country>`.
- `CountryService.kt` — interface (`findAll`, `findByCode`, `findByName`) plus the nested `SortableProperties` enum
  (`CODE`, `NAME`). Contract lives independently of the storage backend.
- `JsonCountryService.kt` — the only current `CountryService` implementation; reads from the injected `countryMap`
  bean and reactively wraps results as `Flux`/`Mono`.
- `CountryController.kt` — `@RestController` at `/countries`. `GET /countries` combines listing and name-lookup
  behind one handler (`name` query param non-empty ⇒ lookup by name, otherwise list all sorted by `sortedBy`, default
  `name`; an unrecognized `sortedBy` value silently falls back to sorting by name). `GET /countries/{code}` looks up
  by two-letter code (validated with `@Pattern`). Missing lookups are turned into `CountryNotFoundException` via
  `switchIfEmpty` and handled centrally.
- `GlobalExceptionHandler.kt` — `@ControllerAdvice` mapping `CountryNotFoundException` → 404 and
  `HandlerMethodValidationException` (e.g. bad `code` pattern) → 400, both as a JSON `Model.ErrorResponse`.
- `Model.kt` — all domain types in one `object Model`: `Country`, `CountryNotFoundException`, `ErrorResponse`, plus
  unused-so-far `Code`/`Name` value classes.
- `SecurityConfiguration.kt` — Spring Security filter chain: `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`, and
  `/countries/**` are `permitAll`; everything else is `denyAll`. Any new endpoint needs an explicit `authorize` rule
  here or it will be denied by default.

Adding a new country data source means implementing `CountryService` and wiring it in in place of
`JsonCountryService` — the controller and exception handling do not need to change.

### Runtime config (`application.yml`)

- Server: port `8081`, context path `/ms-one` (so the API is reachable at `/ms-one/countries`).
- `service.countries.file` points to the classpath JSON resource loaded at startup (`countries.json` for the app,
  `test-countries.json` under `src/test/resources` for tests).
- Actuator endpoints exposed: `health`, `loggers`, `env`, `mappings`.
- Swagger UI / OpenAPI docs at `/ms-one/swagger-ui/index.html` and `/ms-one/v3/api-docs`.

## Testing

- Unit tests (`src/test/kotlin/.../*Test.kt`) run via Surefire in the `test` phase, e.g. `CountryControllerTest.kt`
  uses `WebTestClient.bindToController(...)` with a Mockito-mocked `CountryService` and registers
  `GlobalExceptionHandler` as controller advice — this is the pattern for testing new controller behavior without a
  full application context.
- Integration tests (`src/test/kotlin/.../integration/*IT.kt`, e.g. `ReadJsonFileTestIT.kt`) run via Failsafe in the
  `verify` phase and are named with an `IT` suffix by convention.
- `TestData.kt` centralizes shared fixtures (sample `Country` values, sort-property constants) for controller tests.
- JaCoCo coverage report is written to `target/site/jacoco/` on `verify` and uploaded to Codecov in CI.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs on Java 25 (Zulu) for pushes to `main` and on PR open: restores the
Maven dependency/build-cache, runs `./mvnw -B -V clean verify`, then uploads the JaCoCo report to Codecov.
