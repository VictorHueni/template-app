# Functional Auditing: A Strategic Analysis for a Scalable Architecture

## 1. Introduction

This document provides a deep analysis of three architectural options for implementing functional auditing within the backend service:

1.  **Spring Boot Actuator's `AuditEventRepository`**
2.  **Standard Spring Application Events**
3.  **Spring Modulith's Eventing with Transactional Outbox**

The evaluation is performed through the lens of our core project goals: to create a **modern, cloud-native template application** that is **ready to scale towards a microservices architecture**.

## 2. The Core Challenge: Data Consistency in Distributed Systems

When an application needs to perform two distinct actions—like saving data to a database and publishing an event—it faces the **"dual-write problem"**. In a monolithic application, this is less of a concern. But in a cloud-native or microservices environment, it's a critical failure point.

**Scenario**: A service saves an order to its database, then tries to publish an `OrderCreated` event to a message bus.
-   What if the database `COMMIT` succeeds, but the application crashes before the event is published? The order exists, but no other service will ever know about it. The system is now in an inconsistent state.

A scalable, reliable architecture **must** solve this problem. The solution is the **Transactional Outbox pattern**, which guarantees that a change to business data and the "intent to publish" an event are part of the same atomic database transaction.

## 3. Options Analysis

### Option 1: `AuditEventRepository` (The "Native" Actuator Way)
-   **Analysis**: This is a simple tool for exposing basic operational and security events (e.g., `AUTHENTICATION_SUCCESS`) via an actuator endpoint. Its data model is generic and not designed for rich, domain-specific business events.
-   **Path to Microservices**: **Very poor.** This is an in-process, monolithic solution. It has no mechanism to solve the dual-write problem and provides no pathway for reliable, transactional communication between services. Migrating from this would require a complete rewrite of all auditing logic.
-   **Verdict**: Unsuitable for our strategic goals.

### Option 2: Standard Spring Application Events (`@TransactionalEventListener`)
-   **Analysis**: This pattern decouples components *within a single application*. The `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` annotation ensures the listener is only invoked after the main database transaction succeeds.
-   **Path to Microservices**: **Mediocre.** While it feels event-driven, it **does not solve the dual-write problem** reliably. The event is published and handled *in memory*. If the application crashes between the `COMMIT` and the listener's execution, the event is lost forever.
-   **Architectural "Day 2" Cost**: When the time comes to extract a module into a microservice, you are forced to solve the dual-write problem by manually implementing the Transactional Outbox pattern. This is a significant, complex, and risky refactoring effort that involves creating outbox tables, a polling publisher, and managing event states. It is a large tax to pay for a "simpler" Day 1 implementation.

### Option 3: Spring Modulith (The "Strategic" Way) - Recommended

-   **Analysis**: Spring Modulith is an architectural framework designed to build robust, modular monoliths with a clear evolutionary path. Its eventing mechanism is a first-class implementation of the Transactional Outbox pattern.
-   **How it Works**:
    1.  When you use `ApplicationEventPublisher` in a `@Transactional` method, Spring Modulith intercepts the event.
    2.  It persists a record of the event to a dedicated `event_publication` table *within the same ACID transaction* as your business data.
    3.  **Atomicity is guaranteed**: If the transaction fails, both the business data and the event record are rolled back. The "intent to publish" is never lost.
    4.  After the transaction commits, a separate, reliable process forwards the event from the outbox table to the appropriate `@EventListener` or an external message broker.
-   **Path to Microservices**: **Excellent and explicit.** This is the primary advantage of Spring Modulith.
    -   **"Day 1" Foundation**: It establishes the exact patterns needed for reliable, asynchronous communication between future microservices from the very beginning.
    -   **"Day 2" Migration**: To extract a module (e.g., `billing`) into its own microservice, the process is dramatically simplified:
        1.  The publishing module (e.g., `orders`) **does not change at all**. It continues to publish its `OrderPlaced` event to the local outbox.
        2.  You simply reconfigure the Spring Modulith event forwarder to publish that specific event to an external message broker (like Kafka or RabbitMQ) instead of an internal listener.
        3.  The new `billing` microservice subscribes to the message broker to receive the event.
    -   This approach makes module extraction a low-risk, configuration-driven change rather than a massive refactoring effort.

## 4. Conclusion & Recommendation

For a template application that prioritizes **modern architecture, cloud-native readiness, and a clear path to microservices**, the choice is clear.

**We will adopt Option 3: Spring Modulith.**

While it introduces a slightly higher initial complexity, it is a strategic investment. It forces us to build a well-structured, event-driven monolith from the start and provides the perfect architectural foundation to evolve into a microservices landscape with confidence and minimal friction. Choosing a simpler option now would knowingly introduce significant technical debt that would hinder future scalability.

## 5. References

### Dual-Write Problem & Transactional Outbox
*   [Confluent Blog: The Dual Write Problem](https://www.confluent.io/blog/dual-write-problem-microservices/)
*   [Medium: Microservices Data Consistency with Transactional Outbox Pattern](https://medium.com/@oxyprogrammer/microservices-data-consistency-with-transactional-outbox-pattern-17e997f7663e)
*   [Baeldung: Transactional Outbox Pattern](https://www.baeldung.com/transactional-outbox-pattern)

### Spring Modulith
*   [Spring.io Blog: Introducing Spring Modulith](https://spring.io/blog/2022/02/09/introducing-spring-modulith)
*   [Spring Modulith Documentation](https://docs.spring.io/spring-modulith/docs/current/reference/html/)
*   [Wim Deblauwe: Getting Started with Spring Modulith](https://www.wimdeblauwe.com/blog/2023/02/09/getting-started-with-spring-modulith/)
*   [Baeldung: Spring Modulith Events](https://www.baeldung.com/spring-modulith-events)
*   [Medium: Spring Modulith: A Guide to Building Modular Monoliths](https://medium.com/geekculture/spring-modulith-a-guide-to-building-modular-monoliths-5f9a6e6e22c4)
