# 001. Functional Auditing Strategy

## Context and Problem Statement

Our backend service requires a robust **functional auditing** mechanism to record significant business events (e.g., "GREETING_CREATED", "USER_REGISTERED"). This audit trail is crucial for maintaining a high-level, human-readable log of system activities, distinct from technical auditing or historical data versioning.

The template application is designed with future scalability in mind, targeting a **cloud-native environment** and a potential evolution towards a **microservices architecture**. Therefore, the chosen auditing strategy must align with these long-term architectural goals, particularly addressing challenges related to data consistency in distributed systems (the "dual-write problem").

## Considered Options

*   **Spring Boot Actuator's `AuditEventRepository`**: The "Native" Spring Way.
*   **Standard Spring Application Events (`@TransactionalEventListener`)**: The "Modern" Spring Pattern.
*   **Spring Modulith's Eventing with Transactional Outbox**: The "Strategic" Way.

(For a detailed analysis of these options, refer to `../04-solution-strategy/functional-auditing-options-analysis.md`.)

## Decision Outcome

Chosen option: "**Spring Modulith's Eventing with Transactional Outbox**", because it provides the best strategic fit for our architectural goals, addressing the core challenges of scalability and microservice evolution from day one.

### Consequences

*   **Good**:
    *   **Solves the Dual-Write Problem Natively**: Spring Modulith's built-in [Transactional Outbox pattern](functional-auditing-options-analysis.md#2-the-core-challenge-data-consistency-in-distributed-systems) guarantees atomic persistence of business events with transactional data, ensuring data consistency even in distributed contexts.
    *   **Clear Path to Microservices**: Establishes reliable asynchronous communication patterns essential for future microservice extraction. Modules can be transitioned to independent services with minimal refactoring of event-based interactions.
    *   **Cloud-Native & Resilient**: Provides robust event delivery guarantees, crucial for cloud-native architectures where transient failures are expected.
    *   **Encourages Modular Design**: Promotes and helps enforce a modular structure within the monolith, leading to a cleaner codebase.

*   **Bad**:
    *   **Higher Initial Complexity**: Requires a slightly steeper learning curve and more setup compared to simple Application Events.
    *   **Architectural Commitment**: Adopting Spring Modulith is a commitment to its modularity principles, which influences application structure beyond just eventing.

This decision is a strategic investment that prioritizes long-term architectural health and scalability over minimal immediate implementation effort.

## References

1.  **Spring Modulith Documentation**: [Official Reference for Event Publication](https://docs.spring.io/spring-modulith/docs/current/reference/html/#fundamentals.events)
2.  **Article on Dual-Write and Outbox Pattern**: [Reliable Microservices Data Exchange With the Outbox Pattern](https://www.baeldung.com/transactional-outbox-pattern)
3.  **Video on Spring Modulith by Oliver Drotbohm (Project Lead)**: [More Than Modules - An Introduction to Spring Modulith](https://www.youtube.com/watch?v=s0-b5R2n5vI)
4.  **Article on Microservice Migration**: [Pattern: Monolith to Microservices](https://martinfowler.com/bliki/MonolithFirst.html) by Martin Fowler.