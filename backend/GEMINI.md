# GEMINI.md

## Project Overview

This is a Java Spring Boot application built with Maven. It appears to be a template or demo project that exposes a simple REST API for creating "greetings".

The project follows a clean, layered architecture:
-   **Infrastructure:** Contains components related to external concerns like the web framework (Spring MVC), database access (JPA), etc.
-   **Application:** Contains application services that orchestrate business logic.
-   **Domain:** Contains the core business models and logic, independent of any framework.

**Key Technologies:**
-   **Backend:** Java 25, Spring Boot 3
-   **Build:** Apache Maven
-   **Data:** Spring Data JPA, PostgreSQL (for integration tests), H2 (in-memory, for unit test)
-   **Testing:** JUnit 5, Mockito, REST Assured, Testcontainers
-   **Tooling:** Lombok

## Building and Running

The project uses the Maven Wrapper (`mvnw`), so a local Maven installation is not required.

### Build

To build the project, run all tests, and execute static analysis checks:
```bash
./mvnw clean install
```
On Windows:
```bash
mvnw.cmd clean install
```

### Run

To run the application locally:
```bash
./mvnw spring-boot:run
```
By default, this will use the `local` profile, as defined in `src/main/resources/application.properties`.

Alternatively, you can run the compiled JAR:
```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Test

The project has two types of tests:
-   **Unit Tests:** Files ending in `*Test.java` or `*Tests.java`.
-   **Integration Tests:** Files ending in `*IT.java`.

To run only the unit tests:
```bash
./mvnw test
```

To run both unit and integration tests (this is done during the `install` phase):
```bash
./mvnw verify
```

## Development Conventions

### Code Style
The project uses `checkstyle` with the `google_checks.xml` configuration to enforce a consistent code style. It also uses PMD and SpotBugs for static analysis to find potential bugs and security vulnerabilities.

### Testing
-   **Unit Tests:** Reside in the same package as the code they test and use mocking (Mockito) to isolate the class under test. They are executed by the `maven-surefire-plugin`.
-   **Integration Tests:** Are located in the same module and test the application from the API layer down to the database. They use Testcontainers to spin up a real PostgreSQL database, ensuring tests run in an environment that is very close to production. They are executed by the `maven-failsafe-plugin`.

### API
The API is exposed under the `/api` path. The main endpoint is `POST /api/greetings` which accepts a JSON body like:
```json
{
  "name": "World"
}
```
And returns a greeting object.
