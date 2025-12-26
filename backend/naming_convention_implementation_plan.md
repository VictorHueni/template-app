# Modern PostgreSQL Naming Conventions for Spring Boot

**Version:** 2.0 (Modernized)
**Date:** 2025-12-25
**Philosophy:** Clarity over brevity. Explicit over implicit. Automated over manual.

This document defines the naming standards for the application's PostgreSQL database. It is designed to be **developer-friendly** (compatible with Spring/JPA defaults) and **DBA-friendly** (explicit constraint naming and clear context).

---

## 1. Core Principles

1.  **snake_case Everywhere**: All database objects (tables, columns, indexes, constraints) use `lower_snake_case`.
2.  **No Redundancy**: Do not repeat the table name in the column name (e.g., use `email`, not `user_email`).
3.  **Full Words**: Avoid cryptic abbreviations. Use `account`, not `acct`. Use `status`, not `st`.
4.  **Explicit Constraints**: All constraints (PK, FK, UK) must have meaningful, deterministic names to aid debugging and schema review.
5.  **Module Scoping**: Tables are namespaced by their functional module to organize the schema.

---

## 2. Naming Standards

### 2.1 Tables
**Pattern:** `{module}_{entity}`
**Multiplicity:** Singular (Matches Java Entity Class 1:1)

| Java Entity | Module | Table Name | Why? |
|-------------|--------|------------|------|
| `User` | `auth` | `auth_user` | Scopes 'user' to the auth module. Avoids reserved word `user`. |
| `SalesOrder` | `sales` | `sales_order` | Clear functional area. |
| `OrderItem` | `sales` | `sales_order_item` | Readable hierarchy. |

> **Note:** We prefer **Singular** table names (`auth_user`) because they map directly to the JPA Entity class name. Pluralization logic (User -> Users, Person -> People) introduces unnecessary complexity in the ORM layer.

### 2.2 Columns
**Pattern:** `{attribute}`
**Case:** `snake_case`

| Java Field | Column Name | Bad Example | Reason |
|------------|-------------|-------------|--------|
| `firstName` | `first_name` | `user_first_name` | Redundant. The table is already `auth_user`. |
| `status` | `status` | `st`, `flag` | Be descriptive. |
| `isActive` | `is_active` | `active` | Boolean flags should pose a question (is/has). |
| `createdAt` | `created_at` | `creation_date` | Standardize audit timestamps. |

### 2.3 Primary Keys
**Pattern:** `id`
**Type:** `TSID` (stored as `BIGINT` or `VARCHAR`)

Every table must have a surrogate primary key named `id`.
*   **Why?** It is standard, polymorphic, and predictable. `JOIN auth_user ON auth_user.id` is universally understood.

### 2.4 Foreign Keys
**Pattern:** `{target_entity}_id`

| Relationship | Java Field | Column Name |
|--------------|------------|-------------|
| User has many Orders | `Order.user` | `user_id` |
| Order has many Items | `OrderItem.order` | `order_id` |
| Parent/Child | `Category.parent` | `parent_id` |

### 2.5 Constraints & Indexes
Constraint names must be explicit to ensure error messages (e.g., `duplicate key value violates unique constraint "..."`) are readable by humans.

| Object | Prefix | Pattern | Example |
|--------|:------:|---------|---------|
| **Primary Key** | `pk_` | `pk_{table}` | `pk_auth_user` |
| **Foreign Key** | `fk_` | `fk_{source}_{source_column}_{target}` | `fk_sales_order_order_id_auth_user` |
| **Unique Key** | `uk_` | `uk_{table}_{column}` | `uk_auth_user_email` |
| **Check** | `chk_` | `chk_{table}_{condition}` | `chk_sales_order_positive_total` |
| **Index** | `ix_` | `ix_{table}_{column}` | `ix_sales_order_user_id` |

> **Note on Foreign Keys:** Include the source column name to prevent collisions when multiple FKs reference the same target table. Example: `greeting_aud` has both `rev` and `revend` columns referencing `revinfo`, creating distinct constraint names: `fk_greeting_aud_rev_revinfo` and `fk_greeting_aud_revend_revinfo`.

### 2.6 Sequences
**Pattern:** `seq_{table}_{column}`

Database sequences must have explicit, deterministic names to track and manage them effectively.

| Use Case | Pattern | Example |
|----------|---------|---------|
| **Table ID sequence** | `seq_{table}_id` | `seq_auth_user_id` |
| **Revision sequence** | `seq_{table}_{column}` | `seq_revinfo_id` |
| **Custom sequences** | `seq_{table}_{column}` | `seq_sales_order_sequence_num` |

*Implementation*: Use `@SequenceGenerator` annotations with explicit `sequenceName` parameter:
```java
@Id
@SequenceGenerator(name = "seq_auth_user_id", sequenceName = "seq_auth_user_id", allocationSize = 50)
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_user_id")
@Column(name = "id")
private Long id;
```

---

## 3. Reserved Words & Anti-Patterns

1.  **Reserved Words**: Never use Postgres reserved words as identifiers.
    *   ❌ `user`, `order`, `group`, `offset`, `limit`
    *   ✅ `app_user`, `sales_order`, `user_group`, `page_offset`, `row_limit`
2.  **Generic Names**:
    *   ❌ `data`, `info`, `value`, `type`
    *   ✅ `payload`, `user_info`, `attribute_value`, `account_type`
3.  **Prefixes**: Do not use Hungarian notation or generic type prefixes.
    *   ❌ `tbl_user`, `col_name`, `b_is_active`
    *   ✅ `auth_user`, `name`, `is_active`

---

## 4. Implementation Strategy (Spring Boot)

We enforce these rules using Hibernate's `NamingStrategy` API. We do **not** rely on developers remembering to add `@Table(name="...")` or `@ForeignKey(name="...")`.

### 4.1 Physical Naming Strategy (Tables & Columns)
**Goal:** Convert CamelCase to snake_case and apply Module prefixes.

*   **Logic:**
    1.  Read `@TableModule("auth")` from the Entity.
    2.  Combine Module + Entity Name -> `auth_user`.
    3.  Convert Field Name -> `snake_case`.

### 4.2 Implicit Naming Strategy (Constraints & Sequences)
**Goal:** Generate consistent constraint and sequence names (`pk_`, `fk_`, `ix_`, `seq_`).

*   **Logic:**
    1.  Override `determineForeignKeyName`: Return `fk_` + source_table + `_` + source_column + `_` + target_table.
        - This prevents collisions when multiple FKs reference the same target table.
    2.  Override `determineUniqueKeyName`: Return `uk_` + table + `_` + columns.
    3.  Override `determineIndexName`: Return `ix_` + table + `_` + columns.
    4.  Override `determineSequenceName` (if implementing custom naming): Return `seq_` + table + `_` + column.
        - Note: JPA `@SequenceGenerator` handles explicit sequence naming; Implicit strategy handles Hibernate-auto-generated sequences.

---

## 5. Migration Guide

### 5.1 New Entities
Create your entity and specify the module:

```java
@Entity
@Module("billing") // Custom annotation
public class Invoice {
    // Maps to table: billing_invoice
    // PK: id (pk_billing_invoice)
    
    @Column(nullable = false)
    private BigDecimal totalAmount; 
    // Maps to column: total_amount
}
```

### 5.2 SQL Review Checklist
When reviewing Pull Requests, ensure:

- [ ] Tables are module-scoped (`billing_...`, `auth_...`).
- [ ] No `tbl_` prefixes.
- [ ] No mixedCase identifiers.
- [ ] Foreign keys end in `_id`.
- [ ] Indexes are created for all Foreign Keys (Postgres does not do this automatically).
- [ ] Constraint names follow the `uk_`/`fk_` pattern (including source column for FK collisions).
- [ ] Sequences follow `seq_{table}_{column}` pattern and use `@SequenceGenerator`.

---

## 6. Hibernate Envers Naming Conventions

When using Hibernate Envers for audit history, follow these additional rules:

### 6.1 Audit Table Naming
- **Suffix**: `_aud` (lowercase, snake_case)
- **Example**: `greeting` → `greeting_aud`
- **Configuration**: `spring.jpa.properties.org.hibernate.envers.audit_table_suffix=_aud`

### 6.2 Audit Columns
- **Revision field**: `rev` (maps to `revinfo.rev`)
- **Revision type field**: `revtype` (tracks INSERT/UPDATE/DELETE)
- **Validity columns** (when using `ValidityAuditStrategy`):
  - `revend` (end revision for historical queries)
  - Both `rev` and `revend` FKs reference `revinfo` and must have distinct constraint names:
    - `fk_{audit_table}_rev_revinfo`
    - `fk_{audit_table}_revend_revinfo`

### 6.3 Revision Entity (CustomRevisionEntity)
- Must use explicit `@SequenceGenerator` for `id`:
  ```java
  @SequenceGenerator(name = "seq_revinfo_id", sequenceName = "seq_revinfo_id", allocationSize = 50)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_revinfo_id")
  ```
- Sequence name: `seq_revinfo_id`
- Revision number column: `rev` (not `id` in the database; mapped by `@RevisionNumber`)

### 6.4 Configuration Properties
```properties
# Audit table suffix (lowercase, all lowercase snake_case)
spring.jpa.properties.org.hibernate.envers.audit_table_suffix=_aud

# Revision field names (lowercase)
spring.jpa.properties.org.hibernate.envers.revision_field_name=rev
spring.jpa.properties.org.hibernate.envers.revision_type_field_name=revtype

# Audit strategy: ValidityAuditStrategy adds revend column for range-based queries
spring.jpa.properties.org.hibernate.envers.audit_strategy=org.hibernate.envers.strategy.internal.ValidityAuditStrategy

# Store deleted entity data for complete audit trail
spring.jpa.properties.org.hibernate.envers.store_data_at_delete=true
```

---
