package com.example.demo.greeting.repository;

import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.Revisions;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.common.audit.CustomRevisionEntity;
import com.example.demo.greeting.model.Greeting;
import com.example.demo.testsupport.AbstractIntegrationTest;
import com.example.demo.user.domain.UserDetailsImpl;
import com.example.demo.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

/**
 * Integration test for GreetingRepository with real PostgreSQL database.
 * Uses the singleton Testcontainers PostgreSQL instance from {@link AbstractIntegrationTest}.
 */
@SpringBootTest
@Transactional
@ActiveProfiles({"test", "integration"})
@ResourceLock(value = "DB", mode = READ_WRITE)
class GreetingRepositoryIT extends AbstractIntegrationTest {


    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:devpassword}")
    private String adminPassword;

    @Autowired
    GreetingRepository greetingRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setupSystemUser() {
        txTemplate = new TransactionTemplate(transactionManager);

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            UserDetailsImpl systemUser = new UserDetailsImpl(adminUsername, adminPassword);
            userRepository.saveAndFlush(systemUser);
        }
    }

    @Test
    void savesAndLoadsGreetingFromPostgres() {
        Greeting toSave = new Greeting(
                "Bob",
                "Hello Bob"
        );

        toSave.setReference("GRE-2025-000001");

        Greeting saved = greetingRepository.save(toSave);

        var loaded = greetingRepository.findById(saved.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isNotNull();
        assertThat(loaded.get().getReference()).isEqualTo("GRE-2025-000001");
        assertThat(loaded.get().getRecipient()).isEqualTo("Bob");
        assertThat(loaded.get().getMessage()).isEqualTo("Hello Bob");
    }

    @Test
    void findsGreetingsWithPaginationAndSorting() {
        // 1. Arrange: Clear DB and save 3 greetings with different times
        greetingRepository.deleteAll();

        Instant now = Instant.now();
        Greeting oldGreeting = new Greeting("Old", "First");
        Greeting midGreeting = new Greeting("Mid", "Second");
        Greeting newGreeting = new Greeting("New", "Third");

        oldGreeting.setReference("GRE-2025-000001");
        midGreeting.setReference("GRE-2025-000002");
        newGreeting.setReference("GRE-2025-000003");

        greetingRepository.save(oldGreeting);
        greetingRepository.save(midGreeting);
        greetingRepository.save(newGreeting);

        // 2. Act: Request Page 0 with Size 2
        // Sort by id DESC - TSIDs are time-sorted, so newer records have higher IDs
        // This is more reliable than createdAt which may have identical timestamps for rapid saves
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id"));
        Page<Greeting> result = greetingRepository.findAll(pageRequest);

        // 3. Assert
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        // Verify Sorting (Newest First)
        assertThat(result.getContent().get(0).getMessage()).isEqualTo("Third");
        assertThat(result.getContent().get(1).getMessage()).isEqualTo("Second");
        assertThat(result.getContent().get(0).getReference()).isEqualTo("GRE-2025-000003");
        assertThat(result.getContent().get(1).getReference()).isEqualTo("GRE-2025-000002");
    }

    // ============================================================
    // TDD Tests for CRUD operations (get, update, delete)
    // ============================================================

    @Test
    void findsGreetingById() {
        // Arrange
        Greeting toSave = new Greeting("Alice", "Hello Alice");
        toSave.setReference("GRE-2025-000010");
        Greeting saved = greetingRepository.save(toSave);

        // Act
        var found = greetingRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getReference()).isEqualTo("GRE-2025-000010");
        assertThat(found.get().getRecipient()).isEqualTo("Alice");
        assertThat(found.get().getMessage()).isEqualTo("Hello Alice");
    }

    @Test
    void returnsEmptyWhenGreetingNotFoundById() {
        // Act
        var found = greetingRepository.findById(999999999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void deletesGreetingById() {
        // Arrange
        Greeting toSave = new Greeting("ToDelete", "Delete me");
        toSave.setReference("GRE-2025-000020");
        Greeting saved = greetingRepository.save(toSave);
        Long id = saved.getId();

        // Verify it exists
        assertThat(greetingRepository.existsById(id)).isTrue();

        // Act
        greetingRepository.deleteById(id);

        // Assert
        assertThat(greetingRepository.existsById(id)).isFalse();
        assertThat(greetingRepository.findById(id)).isEmpty();
    }

    @Test
    void existsByIdReturnsTrueWhenExists() {
        // Arrange
        Greeting toSave = new Greeting("Exists", "I exist");
        toSave.setReference("GRE-2025-000030");
        Greeting saved = greetingRepository.save(toSave);

        // Act & Assert
        assertThat(greetingRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsByIdReturnsFalseWhenNotExists() {
        // Act & Assert
        assertThat(greetingRepository.existsById(999999999L)).isFalse();
    }

    // ============================================================
    // JPA Auditing Tests
    // ============================================================

    @Test
    void verifyAuditFieldsArePopulatedOnCreate() {
        // Arrange
        Greeting toSave = new Greeting("AuditTest", "Testing audit fields");
        toSave.setReference("GRE-2025-AUDIT001");
        Instant beforeSave = Instant.now();

        // Act
        Greeting saved = greetingRepository.saveAndFlush(toSave);
        entityManager.clear(); // Clear persistence context to force reload
        Greeting loaded = greetingRepository.findById(saved.getId()).orElseThrow();

        // Assert audit fields
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getCreatedAt()).isAfterOrEqualTo(beforeSave.minusSeconds(1));
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(loaded.getCreatedBy()).isNotNull();
        assertThat(loaded.getCreatedBy()).isEqualTo("anonymous");
        assertThat(loaded.getVersion()).isEqualTo(0);
    }

    @Test
    void verifyAuditFieldsAreUpdatedOnModification() {
        // Arrange: Create entity
        Greeting toSave = new Greeting("Original", "Original message");
        toSave.setReference("GRE-2025-AUDIT002");
        Greeting saved = greetingRepository.saveAndFlush(toSave);
        entityManager.clear();

        Greeting loaded = greetingRepository.findById(saved.getId()).orElseThrow();
        Instant createdAt = loaded.getCreatedAt();

        // Act: Modify entity
        loaded.setMessage("Modified message");
        Greeting updated = greetingRepository.saveAndFlush(loaded);
        entityManager.clear();

        Greeting reloaded = greetingRepository.findById(updated.getId()).orElseThrow();

        // Assert
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt); // Unchanged
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(createdAt); // Updated
        assertThat(reloaded.getVersion()).isEqualTo(1); // Incremented
    }

    // ============================================================
    // Hibernate Envers Audit History Tests
    // ============================================================
    // Note: Envers tests use TransactionTemplate because Envers only creates
    // revisions when transactions actually commit. We need separate transactions
    // for: 1) creating data, 2) querying revisions.

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldCreateAuditRevisionOnSave() {
        String uniqueRef = String.format("GRE-1001-%06d", System.currentTimeMillis() % 1000000);
        AtomicLong greetingId = new AtomicLong();

        // Transaction 1: Create and save greeting
        txTemplate.execute(status -> {
            Greeting greeting = new Greeting("EnversTest", "Initial message");
            greeting.setReference(uniqueRef);
            greeting = greetingRepository.saveAndFlush(greeting);
            greetingId.set(greeting.getId());
            return null;
        });

        // Transaction 2: Query audit history (revision created after TX1 commit)
        txTemplate.execute(status -> {
            Revisions<Integer, Greeting> revisions = greetingRepository.findRevisions(greetingId.get());

            assertThat(revisions).hasSize(1);

            Revision<Integer, Greeting> firstRev = revisions.iterator().next();
            assertThat(firstRev.getMetadata().getRevisionType())
                    .isEqualTo(RevisionMetadata.RevisionType.INSERT);
            assertThat(firstRev.getEntity().getMessage()).isEqualTo("Initial message");
            return null;
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldTrackMultipleRevisions() {
        String uniqueRef = String.format("GRE-1002-%06d", System.currentTimeMillis() % 1000000);
        AtomicLong greetingId = new AtomicLong();

        // Transaction 1: Create greeting
        txTemplate.execute(status -> {
            Greeting greeting = new Greeting("MultiRevTest", "Original");
            greeting.setReference(uniqueRef);
            greeting = greetingRepository.saveAndFlush(greeting);
            greetingId.set(greeting.getId());
            return null;
        });

        // Transaction 2: Update greeting
        txTemplate.execute(status -> {
            Greeting toUpdate = greetingRepository.findById(greetingId.get()).orElseThrow();
            toUpdate.setMessage("Modified");
            greetingRepository.saveAndFlush(toUpdate);
            return null;
        });

        // Transaction 3: Query revisions
        txTemplate.execute(status -> {
            Revisions<Integer, Greeting> revisions = greetingRepository.findRevisions(greetingId.get());

            assertThat(revisions).hasSize(2);

            Iterator<Revision<Integer, Greeting>> iter = revisions.iterator();
            Revision<Integer, Greeting> firstRev = iter.next();
            Revision<Integer, Greeting> secondRev = iter.next();

            assertThat(firstRev.getMetadata().getRevisionType())
                    .isEqualTo(RevisionMetadata.RevisionType.INSERT);
            assertThat(firstRev.getEntity().getMessage()).isEqualTo("Original");

            assertThat(secondRev.getMetadata().getRevisionType())
                    .isEqualTo(RevisionMetadata.RevisionType.UPDATE);
            assertThat(secondRev.getEntity().getMessage()).isEqualTo("Modified");
            return null;
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldCaptureUsernameInRevision() {
        String uniqueRef = String.format("GRE-1003-%06d", System.currentTimeMillis() % 1000000);
        AtomicLong greetingId = new AtomicLong();

        // Transaction 1: Create greeting
        txTemplate.execute(status -> {
            // --- ADD THIS BLOCK ---
            // Simulate a logged-in "system" user
            org.springframework.security.core.userdetails.UserDetails user =
                    org.springframework.security.core.userdetails.User.withUsername("admin")
                            .password("pw")
                            .authorities("ROLE_ADMIN")
                            .build();
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            // ----------------------

            try {
                Greeting greeting = new Greeting("UserTest", "Testing username capture");
                greeting.setReference(uniqueRef);
                greeting = greetingRepository.saveAndFlush(greeting);
                greetingId.set(greeting.getId());
            } finally {
                // Always clean up after setting context manually
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
            return null;
        });

        // Transaction 2: Query revision and verify username
        txTemplate.execute(status -> {
            var lastRevision = greetingRepository.findLastChangeRevision(greetingId.get());

            assertThat(lastRevision).isPresent();

            Object delegate = lastRevision.get().getMetadata().getDelegate();
            assertThat(delegate).isInstanceOf(CustomRevisionEntity.class);

            CustomRevisionEntity customRev = (CustomRevisionEntity) delegate;

            // Assert that the username matches the one we set in Transaction 1
            assertThat(customRev.getUsername()).isEqualTo("system");
            return null;
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldFindSpecificRevision() {
        String uniqueRef = String.format("GRE-1004-%06d", System.currentTimeMillis() % 1000000);
        AtomicLong greetingId = new AtomicLong();
        AtomicInteger firstRevNumber = new AtomicInteger();

        // Transaction 1: Create greeting
        txTemplate.execute(status -> {
            Greeting greeting = new Greeting("SpecificRevTest", "Version 1");
            greeting.setReference(uniqueRef);
            greeting = greetingRepository.saveAndFlush(greeting);
            greetingId.set(greeting.getId());
            return null;
        });

        // Transaction 2: Update greeting
        txTemplate.execute(status -> {
            Greeting toUpdate = greetingRepository.findById(greetingId.get()).orElseThrow();
            toUpdate.setMessage("Version 2");
            greetingRepository.saveAndFlush(toUpdate);
            return null;
        });

        // Transaction 3: Get first revision number
        txTemplate.execute(status -> {
            Revisions<Integer, Greeting> revisions = greetingRepository.findRevisions(greetingId.get());
            firstRevNumber.set(revisions.iterator().next().getMetadata().getRequiredRevisionNumber());
            return null;
        });

        // Transaction 4: Find specific revision
        txTemplate.execute(status -> {
            var specificRevision = greetingRepository.findRevision(greetingId.get(), firstRevNumber.get());

            assertThat(specificRevision).isPresent();
            assertThat(specificRevision.get().getEntity().getMessage()).isEqualTo("Version 1");
            return null;
        });
    }
}
