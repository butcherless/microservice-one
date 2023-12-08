# Microservice One

## Status

![Github CI](https://github.com/butcherless/microservice-one/workflows/CI/badge.svg)

## Proof of concept and research with colleagues and friends.

Goals:

- Service behind a _Gateway_
- Tinker with _Spring Cloud Gateway_

Tech stack:

- Spring Boot 3.2.x
- Kotlin 2.0.x
- Java 21

## New project basic structure

Check _Java_ and _Maven_ versions:

    ./mvnw -v

```bash
mkdir my-multi-module-project
mvn archetype:generate -DgroupId=dev.cmartin -DartifactId=learn-spring-cloud -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
mvn archetype:generate -DgroupId=dev.cmartin -DartifactId=microservice-one -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
mkdir -p src/{main,test}/{java,resources} src/main/java/dev/cmartin/learn
```

## Useful commands

Run the microservice:

    java -jar target/microservice-one-0.0.1-SNAPSHOT.jar

Dependency list

    ./mvnw dependency:list -DincludeGroupIds=org.springframework
    ./mvnw dependency:list -DincludeGroupIds=org.jetbrains.kotlin

Dependency updates

    ./mvnw versions:display-dependency-updates

## HTTP client commands [`httpie`]

| Command                                          | Description                                           |
|--------------------------------------------------|-------------------------------------------------------|
| `http -v ':8081/ms-one/countries'`               | Retrieve all countries sorted by name (default limit) |
| `http -v ':8081/ms-one/countries?sortedBy=code'` | Retrieve all countries sorted by code (default limit) |
| `http -v ':8081/ms-one/countries/es'`            | Retrieve a country by its code                        |
| `http -v ':8081/ms-one/countries?name=Portugal'` | Retrieve a country by its name                        |