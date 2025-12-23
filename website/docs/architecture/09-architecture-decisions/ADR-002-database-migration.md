# 002. Database Migration Strategy

## Context and Problem Statement

Our backend application relies on a relational database (PostgreSQL) to store persistent data. As the application evolves, the database schema (tables, columns, indexes, constraints) must change in sync with the application code.

In a modern CI/CD environment, manual schema updates are error-prone and unscalable. We need a robust mechanism to:
1.  **Version Control**: Manage database schema changes as code alongside the application source.
2.  **Reproducibility**: Ensure that any environment (Dev, Test, Prod) can be reliably recreated or updated to a specific state.
3.  **Automation**: Apply changes automatically during deployment without human intervention.
4.  **PostgreSQL Optimization**: Leverage specific features of our chosen database without fighting an abstraction layer.

## Considered Options

*   **JPA/Hibernate `ddl-auto`**: The "Code-First" mechanism (Standard for local dev).
*   **Liquibase**: The "Database-Agnostic" XML/YAML/JSON abstraction.
*   **Flyway**: The "SQL-First" migration tool.

(For a detailed analysis of these options, refer to `../04-solution-strategy/database-migration-options-analysis.md`.)

## Decision Outcome

Chosen option: "**Flyway**", because it aligns perfectly with our project's values of **rigorous API governance**, **transparency**, and **PostgreSQL optimization**.

### Consequences

*   **Good**:
    *   **Transparency & Control**: Migrations are written in plain SQL. There is no ambiguity about what will be executed against the production database.
    *   **PostgreSQL Native**: We can utilize advanced PostgreSQL features (e.g., partial indexes, JSONB operators, specific constraint types, TSID generation functions) directly in the migration scripts without limitation.
    *   **Simplicity**: The learning curve is minimal for any developer familiar with SQL. Reviewing PRs is straightforward as the diffs are standard SQL.
    *   **Immutable History**: Flyway's append-only model encourages a safe "roll-forward" strategy for fixing issues, which is preferable in distributed systems.

*   **Bad**:
    *   **Database Coupling**: Migration scripts are specific to PostgreSQL. Switching to another database engine (e.g., MySQL or Oracle) would require rewriting all migration scripts. *However, this is a calculated trade-off as we have explicitly chosen PostgreSQL as our standard.*
    *   **No Automated Rollbacks**: Unlike Liquibase, Flyway (Community Edition) does not provide automated rollback generation. Rollbacks must be handled by writing a new "undo" migration or, more commonly, a "fix-forward" migration.

This decision prioritizes **operational correctness** and **native power** over database interchangeability.

## References

1.  **Flyway Documentation**: [Official Documentation](https://flywaydb.org/documentation/)
2.  **Comparison Article**: [Flyway vs Liquibase: Which one to choose?](https://www.red-gate.com/blog/database-devops/flyway-vs-liquibase)
3.  **Spring Boot Integration**: [Execute Flyway Database Migrations on Startup](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration)
4.  **Evolutionary Database Design**: [Article by Martin Fowler](https://martinfowler.com/articles/evodb.html)
