# Microservice One

## Status

![GitHub CI](https://github.com/butcherless/microservice-one/workflows/CI/badge.svg)

Microservice One is a country lookup service built with Spring Boot and Kotlin. It is also used to explore integration
with Spring Cloud Gateway.

The service reads country data from a bundled JSON resource and supports listing and sorting countries, as well as
looking them up by exact name or two-letter code.

## Tech stack

- Spring Boot 4.1.0
- Kotlin 2.4.0
- Java 25
- Maven 3.9.16 through the Maven Wrapper
- Spring MVC, WebFlux, and Project Reactor
- Spring Security and Bean Validation
- Springdoc OpenAPI
- JaCoCo for code coverage
- Apache Maven Build Cache Extension 1.2.3

## Prerequisites

- Java 25
- Git

Maven does not need to be installed separately. Check the Java and Maven versions selected by the wrapper:

```bash
./mvnw --version
```

## Build and test

Run the complete build:

```bash
./mvnw clean verify
```

This compiles the application, runs 10 unit tests and 1 integration test, creates the executable JAR, and generates the
JaCoCo report under `target/site/jacoco/`.

## Run the service

Build and run the executable JAR:

```bash
./mvnw clean package
java -jar target/microservice-one-0.0.1-SNAPSHOT.jar
```

For development, run it directly through Spring Boot:

```bash
./mvnw spring-boot:run
```

The service listens on port `8081` and uses the `/ms-one` context path.

## Useful commands

### List dependencies

```bash
./mvnw dependency:list -DincludeGroupIds=org.springframework
./mvnw dependency:list -DincludeGroupIds=org.jetbrains.kotlin
```

### Check dependency updates

```bash
./mvnw versions:display-dependency-updates
```

### Check plugin updates

```bash
./mvnw versions:display-plugin-updates
```

### Force all tests while retaining cache writes

Bypass existing cache entries, run all unit and integration tests, and save the new result:

```bash
./mvnw clean verify -Dunit.test.skip=false -Dmaven.build.cache.skipCache=true
```

### Force all tests with caching disabled

Disable both cache reads and writes:

```bash
./mvnw clean verify -Dunit.test.skip=false -Dmaven.build.cache.enabled=false
```

### Maven build cache

A normal `clean verify` restores matching build results from the local cache when possible:

```bash
./mvnw clean verify
```

Local cache entries are stored under `~/.m2/build-cache`. The project retains up to three cached builds per artifact. CI
persists both `~/.m2/repository` and `~/.m2/build-cache`.

## Country API

Base URL:

```text
http://localhost:8081/ms-one/countries
```

The service returns at most 20 countries when listing them. The `sortedBy` parameter accepts `name` or `code`; an
unsupported value falls back to sorting by name. Country-name lookups require an exact match. Codes must contain two
letters. Invalid codes return HTTP `400`; missing countries return HTTP `404` with a JSON error response.

### HTTPie examples

| Command                                          | Description                                        |
|--------------------------------------------------|----------------------------------------------------|
| `http -v ':8081/ms-one/countries'`               | Retrieve up to 20 countries sorted by name         |
| `http -v ':8081/ms-one/countries?sortedBy=code'` | Retrieve up to 20 countries sorted by code         |
| `http -v ':8081/ms-one/countries/es'`            | Retrieve a country by its two-letter code          |
| `http -v ':8081/ms-one/countries?name=Portugal'` | Retrieve a country by its exact name               |

Equivalent request definitions for JetBrains HTTP Client are available in `country-controller.http`.

## Actuator endpoints

The application exposes the following actuator endpoints:

```bash
http -v ':8081/ms-one/actuator/health'
http -v ':8081/ms-one/actuator/loggers'
http -v ':8081/ms-one/actuator/env'
http -v ':8081/ms-one/actuator/mappings'
```

## OpenAPI documentation

With the service running, use these endpoints:

```text
http://localhost:8081/ms-one/swagger-ui/index.html
http://localhost:8081/ms-one/v3/api-docs
```

## Continuous integration

GitHub Actions runs the project on Java 25. The CI job:

1. Restores Maven dependencies and build-cache entries.
2. Runs `./mvnw -B -V clean verify`.
3. Uploads the generated JaCoCo coverage report to Codecov.
