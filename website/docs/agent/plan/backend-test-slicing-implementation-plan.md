# Implementation Plan: Test Slicing & Spring Boot Testing Best Practices (Next Step)

## 1. Understanding the Problem

### Why are we doing this?
Even after we speed up “true” Integration Tests (ITs) via parallelization and improved isolation, the test suite will still be slower than necessary if we keep using `@SpringBootTest` for scenarios that don’t require a full application.

Spring Boot test slicing (as recommended in the Zalando article on testing efficiency) reduces feedback time by loading only the beans required for the layer under test.

### What is the current bottleneck?
Today, several tests boot a full Spring context (and sometimes a web server) even when the intent is only:
- MVC contract verification (request mapping, validation, JSON shape)
- JPA mapping/query semantics
- Small “pure Java” logic

This causes:
- Unnecessary startup time
- More memory pressure
- More opportunities to accidentally fragment Spring TestContext caching

### Before vs After (high level)

| Category        | Before                                                                        | After                                                                                          |
| --------------- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| MVC tests       | Many REST CRUD assertions inside `@SpringBootTest(RANDOM_PORT)` + RestAssured | Majority moved to `@WebMvcTest` (MockMvc, mocked service); keep a small end-to-end HTTP subset |
| JPA tests       | Repositories/services often tested via `@SpringBootTest`                      | Repositories tested via `@DataJpaTest` against Postgres; service ITs minimized/targeted        |
| Unit tests      | Some tests still use Spring unnecessarily                                     | Unit tests are pure JUnit/Mockito, no Spring                                                   |
| Context caching | Easy to fragment via per-class profiles/imports/mocks                         | Guardrails: stable profiles, shared configs, minimal per-class variance                        |

---

## 2. Phase 0: Prerequisites & Configuration

### Step 2.1: Baseline and classify tests (taxonomy)
- **Why**: You can’t slice intentionally without agreeing on what each tier proves.
- **What**: Introduce a small “test taxonomy” that explains when to use:
  - Unit tests (JUnit/Mockito, no Spring)
  - MVC slice tests (`@WebMvcTest`)
  - JPA slice tests (`@DataJpaTest`)
  - Full integration tests (`@SpringBootTest`, sometimes `RANDOM_PORT`)
- **Implementation Options & Design Decisions**:
  - Option A: Document-only rule set (fast, junior-friendly)
  - Option B: Document + enforcement (ArchUnit/custom checks) (stronger, do later)
  
- **Changes**:
  - Add a short doc: `website/docs/developer-guides/testing/test-tiers.md` (or similar location you prefer).
  - Reference concrete examples in this repo (see Phase 1–3).

- **Verification**:
  - Manual: reviewer checklist exists and is used in PRs.

### Step 2.2: Guardrails to preserve Spring TestContext caching
- **Why**: Spring caching is a major speed lever; minor annotation differences can create new cache keys and re-bootstrap contexts.
- **What**: Add non-negotiable rules for new/modified tests.

**Guardrails (apply to all Spring-based tests):**
1. **No `@DirtiesContext`** unless there is no alternative.
2. **Profiles must be consistent** within each test tier:
   - Standard MVC/JPA slices should usually use a single profile set (prefer `test`).
   - Full ITs should share one canonical profile set (prefer `test` + `integration` only if truly required across all ITs).
3. Avoid per-class `@TestPropertySource` / `@SpringBootTest(properties = ...)` unless the property is stable across a whole tier.
4. Avoid varying `@Import` / `@ContextConfiguration` per class in full ITs.
5. Avoid `@MockBean` in full ITs (it changes the context). Prefer real wiring + stable test configs.
6. If a mock is required, centralize it in a shared `@TestConfiguration` imported consistently.

- **Implementation Options & Design Decisions**:
  - Option A: Document-only guardrails and code review enforcement (start here)
  - Option B: Add an ArchUnit “testing rules” suite later (optional)

- **Changes**:
  - Add a “Testing Guardrails” section to the test-tier doc.
  - Mirror the most critical guardrails into the IT-parallelization plan.

- **Verification**:
  - Manual: Run tests once and check logs show minimal context boots.
  - Automated (optional): Add a PR checklist item: “Does this introduce new profiles/imports/mocks that fragment the cache?”

---

## 3. Phase 1: Quick Wins (Remove unnecessary Spring)

### Step 3.1: Convert MapStruct mapper test to a pure unit test
- **Why**: Booting Spring for a mapper is pure overhead.
- **What**: Change `backend/src/test/java/com/example/demo/greeting/mapper/GreetingMapperTest.java` to instantiate the mapper directly.

- **Implementation Options & Design Decisions**:
  - Option A (preferred): `new GreetingMapperImpl()`
  - Option B: `Mappers.getMapper(GreetingMapper.class)`

- **Changes**:
  - Remove `@SpringBootTest` from `GreetingMapperTest`.
  - Construct mapper directly and keep assertions.

- **Verification**:
  - Run: `./mvnw test -pl backend -Dtest=GreetingMapperTest`
  - Expected: `BUILD SUCCESS`

### Step 3.2 (Optional): Remove misleading Spring annotations from unit tests
- **Why**: `@ActiveProfiles` on a Mockito test doesn’t do anything; it confuses juniors.
- **What**: Remove `@ActiveProfiles` from `backend/src/test/java/com/example/demo/user/model/UserDetailsImplTest.java` if present.
- **Implementation Options & Design Decisions**:
  - Option A: Keep (no behavior impact)
  - Option B (preferred): Remove for clarity
- **Verification**:
  - Run unit tests: `./mvnw test -pl backend -Dtest=UserDetailsImplTest`

---

## 4. Phase 2: JPA Slice Refactor (`@DataJpaTest`)

### Step 4.1: Convert repository integration tests to `@DataJpaTest`
- **Why**: Repository tests should validate mappings/queries against Postgres without full application startup.
- **What**: Refactor `backend/src/test/java/com/example/demo/greeting/repository/GreetingRepositoryIT.java`.

- **Implementation Options & Design Decisions**:
  - Option A (preferred): `@DataJpaTest` + reuse Testcontainers datasource wiring via the existing `TestcontainersConfiguration`.
  - Option B: Keep `@SpringBootTest` but trim auto-config (still heavier than needed).

- **Changes**:
  - Replace `@SpringBootTest` with `@DataJpaTest`.
  - Ensure it still uses real Postgres (do not replace datasource with an embedded one).
  - Keep only the minimal imports needed for auditing/envers (if applicable).

- **Verification**:
  - Run: `./mvnw test -pl backend -Dtest=GreetingRepositoryIT`
  - Expected: `BUILD SUCCESS`

### Step 4.2: Decide how to test service + persistence behavior without full context
- **Why**: Service tests often duplicate repository tests; keep only what unit tests cannot prove.
- **What**: Re-evaluate `backend/src/test/java/com/example/demo/greeting/service/GreetingServiceIT.java`.

- **Implementation Options & Design Decisions**:
  - Option A: Convert to unit test with mocked repository (fast), plus keep repository `@DataJpaTest` for DB semantics.
  - Option B: Keep a *small* service integration test if it validates behavior that requires DB constructs (e.g., sequences/functional IDs).

- **Changes**:
  - If kept, reduce scope to the “DB-requiring” behaviors only.

- **Verification**:
  - Run: `./mvnw test -pl backend -Dtest=GreetingServiceIT`

---

## 5. Phase 3: MVC Slice Refactor (`@WebMvcTest`)

### Step 5.1: Migrate most greeting controller CRUD tests to `@WebMvcTest`
- **Why**: Most endpoint tests validate MVC mapping + JSON + validation and do not need a server + DB.
- **What**: Create a new MVC slice test suite for greetings and reduce the size of the full RestAssured IT.

- **Implementation Options & Design Decisions**:
  - Option A (preferred): `@WebMvcTest(GreetingController.class)` + `MockMvc` + mock service layer.
  - Option B: Keep existing `@SpringBootTest(RANDOM_PORT)` controller ITs (slower; use only for a small smoke suite).

- **Changes**:
  - Extract request/response contract assertions from `backend/src/test/java/com/example/demo/greeting/controller/GreetingControllerIT.java` into the new slice test.
  - Keep a minimal RestAssured test subset for:
    - “app wiring + serialization” sanity
    - optional OpenAPI response validation (if desired to keep at HTTP level)

- **Verification**:
  - Run new MVC slice tests: `./mvnw test -pl backend -Dtest='*Greeting*WebMvc*'`
  - Run the reduced IT: `./mvnw test -pl backend -Dtest=GreetingControllerIT`

### Step 5.2: Keep security end-to-end tests minimal and intentional
- **Why**: Real-token security tests are expensive; keep them, but keep them few.
- **What**:
  - Keep `backend/src/test/java/com/example/demo/user/controller/UserControllerSecuredIT.java` and `backend/src/test/java/com/example/demo/greeting/controller/GreetingControllerSecuredIT.java` as end-to-end security tests.
  - Move any non-security mapping assertions into `@WebMvcTest` where possible.

- **Implementation Options & Design Decisions**:
  - Option A: “Two-tier security testing”: few full security ITs + many fast MVC tests.
  - Option B: All security tested end-to-end (slow; not recommended).

- **Verification**:
  - Run: `./mvnw test -pl backend -Dtest='*SecuredIT'`

---

## 6. Phase 4: Verification & Regression Prevention

### Step 6.1: Add a simple speed regression check (lightweight)
- **Why**: After slicing, it’s easy to drift back to `@SpringBootTest`.
- **What**: Add a short PR checklist and optionally a note in docs: “Do not introduce new full-context tests unless required.”

- **Implementation Options & Design Decisions**:
  - Option A (preferred): Docs + review checklist (no tooling)
  - Option B: Add enforcement via ArchUnit later

- **Verification**:
  - Run full backend tests once: `./mvnw test -pl backend`
  - Expected: `BUILD SUCCESS`
