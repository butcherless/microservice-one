# Microservice One

## Status

![Github CI](https://github.com/butcherless/microservice-one/workflows/CI/badge.svg)

## Proof of concept and research with colleagues and friends.

Goals:

- Country Service behind a _Gateway_
- Tinker with _Spring Cloud Gateway_

Tech stack:

- Spring Boot 3.2.x
- Kotlin 2.0.x
- Java 21

## New project basic structure

Check _Java_ and _Maven_ versions:

```bash
./mvnw -v
```

```bash
mkdir my-multi-module-project
mvn archetype:generate -DgroupId=dev.cmartin -DartifactId=learn-spring-cloud -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
mvn archetype:generate -DgroupId=dev.cmartin -DartifactId=microservice-one -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
mkdir -p src/{main,test}/{java,resources} src/main/java/dev/cmartin/learn
```

## Useful commands

Run the microservice:

```bash
java -jar target/microservice-one-0.0.1-SNAPSHOT.jar
```

Dependency list

```bash
./mvnw dependency:list -DincludeGroupIds=org.springframework
./mvnw dependency:list -DincludeGroupIds=org.jetbrains.kotlin
```

Dependency updates

```bash
./mvnw versions:display-dependency-updates
```

Plugin updates

```bash
./mvnw versions:display-plugin-updates
```

## HTTP client commands [`httpie`, `curl`]

```bash
curl -v http://localhost:8081/actuator/health | jq
```

| Command                                          | Description                                                     |
|--------------------------------------------------|-----------------------------------------------------------------|
| `http -v ':8081/ms-one/countries'`               | Retrieve all countries sorted by name (default limit)           |
| `http -v ':8081/ms-one/countries?sortedBy=code'` | Retrieve all countries sorted by tow-digit code (default limit) |
| `http -v ':8081/ms-one/countries/es'`            | Retrieve a country by its two-digit code                        |
| `http -v ':8081/ms-one/countries?name=Portugal'` | Retrieve a country by its name                                  |