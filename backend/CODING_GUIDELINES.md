# Backend Developer Guidelines

## 1. Introduction

### 1.1. Philosophy

This guide provides a strict set of coding standards for Java and Spring Boot development in this project. The guiding principles are adapted from the **Google Java Style Guide** and **Spring Framework best practices**. Our goal is to produce code that is:

-   **Readable and Unambiguous**: Code is read more often than it is written.
-   **Consistent**: Consistency creates a predictable and maintainable codebase.
-   **Modern and Robust**: Leverage modern Java features and established Spring patterns to build reliable software.
-   **API-First**: The OpenAPI specification is the contract and single source of truth.

## 2. Source File Structure

-   **File Name**: `PascalCase.java` (e.g., `GreetingController.java`).
-   **File Encoding**: UTF-8.
-   **Import Statements**:
    -   **No Wildcard Imports**: Do not use wildcard imports (e.g., `import java.util.*;`).
    -   **Ordering**: Imports MUST be ordered as follows, with each group separated by a blank line:
        1.  `java.*`
        2.  `jakarta.*`
        3.  `org.*` (Spring, Hibernate, etc.)
        4.  `com.*` (all other third-party libraries)
        5.  `com.example.demo.*` (internal project packages)
        6.  `static` imports (last)

## 3. Formatting

-   **Braces**: Use K&R style (Kernighan & Ritchie). Braces open on the same line and close on a new line.
  ```java
  // Good
  if (condition) {
      // ...
  }
  
  // Bad
  if (condition)
  {
      // ...
  }
  ```
-   **Indentation**: **4 spaces**. No tabs.
-   **Column Limit**: **120 characters**.
-   **Method Chaining**: When wrapping chained calls, each subsequent call should be on a new line, indented by 8 spaces (two indents).
  ```java
  return service.getGreeting(idLong)
          .map(this::toGreetingResponse)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
  ```

## 4. Naming Conventions

### 4.1. Packages
-   Packages MUST be organized by **feature** (`com.example.demo.greeting`), not by layer.

### 4.2. Classes & Records
-   **Controllers**: `PascalCase` with a `Controller` suffix (e.g., `GreetingController`).
-   **Services**: `PascalCase` with a `Service` suffix (e.g., `GreetingService`).
-   **Repositories**: `PascalCase` interface with a `Repository` suffix (e.g., `GreetingRepository`).
-   **JPA Entities**: `PascalCase` noun. No suffix (e.g., `Greeting`).
-   **DTOs**: `PascalCase` and **MUST** be implemented as a Java `record`.
    -   Requests: `*Request` suffix (e.g., `CreateGreetingRequest`).
    -   Responses: `*Response` suffix (e.g., `GreetingResponse`).
    -   General DTOs: `*Dto` suffix (e.g., `GreetingDto`).
-   **Mappers**: `PascalCase` with a `Mapper` suffix (e.g., `GreetingMapper`).
-   **Test Classes**:
    -   Unit Tests: Suffix with `Test` (e.g., `GreetingServiceTest`).
    -   Integration Tests: Suffix with `IT` (e.g., `GreetingControllerIT`).

### 4.3. Methods
-   `camelCase`, verb-first (e.g., `findById`, `createGreeting`).
-   Boolean methods MUST use `is...`, `has...`, or `exists...` prefixes.

### 4.4. Variables
-   **Constants (`static final`)**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRIES`).
-   **All other fields and variables**: `camelCase`.

## 5. Programming Practices & Patterns

### 5.1. Dependency Injection
-   **Constructor Injection is MANDATORY**.
-   Injected fields **MUST** be `private final`.
-   The class **MUST** be annotated with `@RequiredArgsConstructor` from Lombok.
-   **Field injection (`@Autowired` on fields) is FORBIDDEN.**

```java
// Good
@Service
@RequiredArgsConstructor // Automatically creates the constructor for final fields
public class GreetingService {
    private final GreetingRepository repository;
    private final FunctionalIdGenerator idGenerator;
}

// Bad: No final fields, manual constructor
@Service
public class GreetingService {
    private GreetingRepository repository;
    public GreetingService(GreetingRepository repository) { this.repository = repository; }
}

// Bad: Field injection
@Service
public class GreetingService {
    @Autowired
    private GreetingRepository repository;
}
```

### 5.2. Architecture & Layering
1.  **Controller Layer**:
    -   **MUST** implement the OpenAPI-generated interface.
    -   Handles HTTP concerns only (request validation, DTO conversion, response status).
    -   Delegates all business logic to the service layer.
    -   **MUST NOT** access the Repository layer directly.
2.  **Service Layer**:
    -   Contains all business logic.
    -   Defines transaction boundaries (`@Transactional`).
    -   **MUST** operate exclusively on domain/entity objects. It **MUST NOT** accept or return API DTOs.
3.  **Repository Layer**:
    -   Interface-based data access only. Extends a Spring Data interface.

### 5.3. DTOs and Entity Mapping
-   **Strict Separation**: Entities (`Greeting`) and API DTOs (`GreetingResponse`) MUST remain separate.
-   **MapStruct is MANDATORY**: All mapping between DTOs and Entities MUST be done using MapStruct. Manual mapping (like the `toGreetingResponse` method in `GreetingController`) is forbidden and must be refactored.

```java
// Good: Define a mapper interface
@Mapper(componentModel = "spring")
public interface GreetingMapper {
    GreetingResponse toGreetingResponse(Greeting greeting);
    // Add other mappings as needed
}

// In the Controller/Service:
@RequiredArgsConstructor
public class GreetingController {
    private final GreetingService service;
    private final GreetingMapper mapper; // Inject the mapper

    public ResponseEntity<GreetingResponse> getGreeting(String id) {
        return service.getGreeting(Long.parseLong(id))
                .map(mapper::toGreetingResponse) // Use the mapper
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

### 5.4. Use of `Optional`
-   **Return Types**: Services and repositories SHOULD return `Optional<T>` when a result may not be found (e.g., `findById`).
-   **Controller Layer**: The controller MUST handle the `Optional` and return a `404 Not Found` response, as is currently done correctly in `GreetingController`.
-   **Forbidden Uses**: **NEVER** use `Optional` for method parameters or class fields.

### 5.5. Modern Java
-   **`var`**: Use `var` for local variables where the type is obvious from the right-hand side of the expression and it improves readability.
-   **Records**: As stated in 4.2, all DTOs **MUST** be Java `record` types for immutability and conciseness.
-   **Pattern Matching**: Use pattern matching for `instanceof` where it simplifies code.

## 6. Lombok Usage
-   **`@RequiredArgsConstructor`**: **MANDATORY** on Spring components for DI.
-   **`@Getter`, `@Setter`**: **ALLOWED** on individual fields in entities. Avoid class-level usage.
-   **`@Builder`**: **RECOMMENDED** for creating test data and complex objects.
-   **`@Slf4j`**: **RECOMMENDED** for adding a logger to any class.
-   **`@Data`**: **FORBIDDEN** on `@Entity` classes.
-   **`@EqualsAndHashCode`**: **FORBIDDEN** on `@Entity` classes. Use the implementation from `AbstractBaseEntity`.

---
This guide establishes a strict, modern, and enforceable standard for our backend code.
