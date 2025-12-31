package com.example.demo.greeting.service;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.greeting.model.Greeting;
import com.example.demo.testsupport.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

@SpringBootTest
@ActiveProfiles({"test", "integration"})
@Transactional
@ResourceLock(value = "DB", mode = READ_WRITE)
class GreetingServiceIT extends AbstractIntegrationTest {
    @Autowired
    private GreetingService greetingService;

    @Test
    void createsGreetingWithGeneratedIds() {
        // When - Create greeting through service (which generates functional ID)
        Greeting greeting = greetingService.createGreeting("Hello Integration", "TestContainer");

        // Then - Verify both IDs were generated
        assertThat(greeting.getId()).isNotNull(); // TSID
        assertThat(greeting.getReference()).isNotNull(); // Functional ID from sequence
        assertThat(greeting.getReference()).matches("GRE-\\d{4}-\\d{6}");
        assertThat(greeting.getMessage()).isEqualTo("Hello Integration");
        assertThat(greeting.getRecipient()).isEqualTo("TestContainer");
    }

    // ============================================================
    // Integration Tests for CRUD operations (get, update, patch, delete)
    // ============================================================

    @Test
    void getsGreetingById() {
        // Arrange
        Greeting created = greetingService.createGreeting("Hello Get", "GetTest");

        // Act
        var result = greetingService.getGreeting(created.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(created.getId());
        assertThat(result.get().getReference()).isEqualTo(created.getReference());
        assertThat(result.get().getMessage()).isEqualTo("Hello Get");
        assertThat(result.get().getRecipient()).isEqualTo("GetTest");
    }

    @Test
    void returnsEmptyWhenGreetingNotFound() {
        // Act
        var result = greetingService.getGreeting(999999999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void updatesGreetingFully() {
        // Arrange
        Greeting created = greetingService.createGreeting("Original", "OriginalRecipient");

        // Act
        var result = greetingService.updateGreeting(created.getId(), "Updated", "UpdatedRecipient");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(created.getId());
        assertThat(result.get().getReference()).isEqualTo(created.getReference()); // unchanged
        assertThat(result.get().getMessage()).isEqualTo("Updated");
        assertThat(result.get().getRecipient()).isEqualTo("UpdatedRecipient");
    }

    @Test
    void updateReturnsEmptyWhenGreetingNotFound() {
        // Act
        var result = greetingService.updateGreeting(999999999L, "Updated", "UpdatedRecipient");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void patchesOnlyMessage() {
        // Arrange
        Greeting created = greetingService.createGreeting("Original", "OriginalRecipient");

        // Act
        var result = greetingService.patchGreeting(created.getId(), "Patched", null);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).isEqualTo("Patched");
        assertThat(result.get().getRecipient()).isEqualTo("OriginalRecipient"); // unchanged
    }

    @Test
    void patchesOnlyRecipient() {
        // Arrange
        Greeting created = greetingService.createGreeting("Original", "OriginalRecipient");

        // Act
        var result = greetingService.patchGreeting(created.getId(), null, "PatchedRecipient");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).isEqualTo("Original"); // unchanged
        assertThat(result.get().getRecipient()).isEqualTo("PatchedRecipient");
    }

    @Test
    void patchesBothFields() {
        // Arrange
        Greeting created = greetingService.createGreeting("Original", "OriginalRecipient");

        // Act
        var result = greetingService.patchGreeting(created.getId(), "Patched", "PatchedRecipient");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).isEqualTo("Patched");
        assertThat(result.get().getRecipient()).isEqualTo("PatchedRecipient");
    }

    @Test
    void patchReturnsEmptyWhenGreetingNotFound() {
        // Act
        var result = greetingService.patchGreeting(999999999L, "Patched", null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void deletesGreeting() {
        // Arrange
        Greeting created = greetingService.createGreeting("To Delete", "DeleteTest");
        Long id = created.getId();

        // Act
        boolean deleted = greetingService.deleteGreeting(id);

        // Assert
        assertThat(deleted).isTrue();
        assertThat(greetingService.getGreeting(id)).isEmpty();
    }

    @Test
    void deleteReturnsFalseWhenGreetingNotFound() {
        // Act
        boolean deleted = greetingService.deleteGreeting(999999999L);

        // Assert
        assertThat(deleted).isFalse();
    }
}
