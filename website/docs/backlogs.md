# Engineering Backlog

This document tracks planned architectural refinements, technical debt, and feature enhancements for the project.

---

## легенда (Legend)

*   **Priority**: 🟥 High, 🟧 Medium, 🟦 Low
*   **Effort**: 👕 T-Shirt Size (S, M, L, XL)

---

## 📋 To Do

This section lists tasks that are approved and ready for development.

### Backend

---

#### [Refactor] Enforce MapStruct for DTO-Entity Mapping

*   **Priority**: 🟧 Medium
*   **Effort**: 👕 M
*   **Description**: The current implementation uses manual mapping logic within controllers and services to convert between JPA Entities and API DTOs. This violates the architectural principle of separation of concerns and introduces boilerplate code. This task is to refactor the existing code to use MapStruct, our mandatory mapping library.

*   **Required Steps**:
    1.  **Add Dependencies**: Update the `pom.xml` to include `org.mapstruct:mapstruct` and the annotation processor `org.mapstruct:mapstruct-processor`.
    2.  **Configure Compiler**: Configure the `maven-compiler-plugin` to correctly use the MapStruct annotation processor during the build lifecycle.
    3.  **Implement Mappers**: For each feature (e.g., `greeting`), create a `GreetingMapper` interface annotated with `@Mapper(componentModel = "spring")`.
    4.  **Refactor Services/Controllers**: Inject the generated mapper (e.g., `GreetingMapper`) and replace all manual conversion logic with calls to the mapper methods (e.g., `mapper.toDto(entity)`).
    5.  **Cleanup**: Remove the old manual mapping methods.

---

#### [Refactor] Isolate Service Layer from DTOs

*   **Priority**: 🟦 Low
*   **Effort**: 👕 S
*   **Description**: The `TECH_GUIDE` specifies that the service layer must only operate on and return JPA entities or primitive types, never DTOs. Currently, the `GreetingService` violates this rule by creating and returning a `GreetingRevisionDTO` for the audit history feature. This mixes presentation concerns with business logic.

*   **Required Steps**:
    1.  Ensure a `GreetingRevisionMapper` (or similar) exists, created as part of the MapStruct refactoring.
    2.  Refactor the `GreetingService`'s audit methods (`getGreetingHistory`, etc.) to return the `Revision<Integer, Greeting>` object or another pure entity representation.
    3.  Create a new service or controller method that is responsible for calling the `GreetingService` and then using the mapper to convert the result into a `GreetingRevisionDTO` for the consumer.

---

### Frontend

*(No items yet)*

---

## 🏃 In Progress

This section lists tasks that are currently being worked on.

*(No items yet)*

---

## ✅ Done

This section lists tasks that have been completed.

*(No items yet)*
