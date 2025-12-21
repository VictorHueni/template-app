package com.example.demo.common.domain;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AbstractBaseEntity")
class AbstractBaseEntityTest {

    /**
     * Concrete test entity implementation for testing the abstract base class
     */
    private static class TestEntity extends AbstractBaseEntity {
        // No additional fields needed for testing
    }

    /**
     * Helper method to set ID via reflection (following existing test patterns)
     */
    private void setIdViaReflection(AbstractBaseEntity entity, Long id) throws Exception {
        Field field = AbstractBaseEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Nested
    @DisplayName("equals")
    class EqualsContract {

        @Test
        @DisplayName("should be reflexive - entity equals itself")
        void shouldBeReflexive() throws Exception {
            // Arrange
            TestEntity entity = new TestEntity();
            setIdViaReflection(entity, 1L);

            // Act & Assert
            assertThat(entity.equals(entity)).isTrue();
        }

        @Test
        @DisplayName("should be symmetric - a.equals(b) == b.equals(a)")
        void shouldBeSymmetric() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 1L);
            setIdViaReflection(entity2, 1L);

            // Act & Assert
            assertThat(entity1.equals(entity2)).isTrue();
            assertThat(entity2.equals(entity1)).isTrue();
        }

        @Test
        @DisplayName("should be transitive - if a.equals(b) and b.equals(c), then a.equals(c)")
        void shouldBeTransitive() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            TestEntity entity3 = new TestEntity();
            setIdViaReflection(entity1, 1L);
            setIdViaReflection(entity2, 1L);
            setIdViaReflection(entity3, 1L);

            // Act & Assert
            assertThat(entity1.equals(entity2)).isTrue();
            assertThat(entity2.equals(entity3)).isTrue();
            assertThat(entity1.equals(entity3)).isTrue();
        }

        @Test
        @DisplayName("should be consistent - multiple invocations return same result")
        void shouldBeConsistent() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 1L);
            setIdViaReflection(entity2, 1L);

            // Act & Assert - call multiple times
            assertThat(entity1.equals(entity2)).isTrue();
            assertThat(entity1.equals(entity2)).isTrue();
            assertThat(entity1.equals(entity2)).isTrue();
        }

        @Test
        @DisplayName("should return false for null")
        void shouldReturnFalseForNull() throws Exception {
            // Arrange
            TestEntity entity = new TestEntity();
            setIdViaReflection(entity, 1L);

            // Act & Assert
            assertThat(entity.equals(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for different class")
        void shouldReturnFalseForDifferentClass() throws Exception {
            // Arrange
            TestEntity entity = new TestEntity();
            setIdViaReflection(entity, 1L);
            String differentType = "Not an entity";

            // Act & Assert
            assertThat(entity.equals(differentType)).isFalse();
        }

        @Test
        @DisplayName("should return true for same ID")
        void shouldReturnTrueForSameId() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 100L);
            setIdViaReflection(entity2, 100L);

            // Act & Assert
            assertThat(entity1.equals(entity2)).isTrue();
        }

        @Test
        @DisplayName("should return false for different ID")
        void shouldReturnFalseForDifferentId() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 1L);
            setIdViaReflection(entity2, 2L);

            // Act & Assert
            assertThat(entity1.equals(entity2)).isFalse();
        }

        @Test
        @DisplayName("should return false when one ID is null")
        void shouldReturnFalseWhenOneIdIsNull() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 1L);
            // entity2 has null ID

            // Act & Assert
            assertThat(entity1.equals(entity2)).isFalse();
            assertThat(entity2.equals(entity1)).isFalse();
        }

        @Test
        @DisplayName("should return false when both IDs are null but different instances")
        void shouldReturnFalseWhenBothIdsAreNullAndDifferentInstances() {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            // both have null IDs

            // Act & Assert - different instances with null IDs should not be equal
            assertThat(entity1.equals(entity2)).isFalse();
        }

        @Test
        @DisplayName("should return true when same instance even with null ID")
        void shouldReturnTrueWhenSameInstance() {
            // Arrange
            TestEntity entity = new TestEntity();

            // Act & Assert
            assertThat(entity.equals(entity)).isTrue();
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCodeContract {

        @Test
        @DisplayName("should be consistent with equals - equal objects have same hash code")
        void shouldBeConsistentWithEquals() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 1L);
            setIdViaReflection(entity2, 1L);

            // Act & Assert
            assertThat(entity1.equals(entity2)).isTrue();
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("should return same value for equal objects")
        void shouldReturnSameValueForEqualObjects() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 42L);
            setIdViaReflection(entity2, 42L);

            // Act
            int hash1 = entity1.hashCode();
            int hash2 = entity2.hashCode();

            // Assert
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should handle null ID - return class hash code")
        void shouldHandleNullId() {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            // both have null IDs

            // Act
            int hash1 = entity1.hashCode();
            int hash2 = entity2.hashCode();

            // Assert - should both return class hash code
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1).isEqualTo(TestEntity.class.hashCode());
        }

        @Test
        @DisplayName("should be consistent - multiple invocations return same value")
        void shouldBeConsistent() throws Exception {
            // Arrange
            TestEntity entity = new TestEntity();
            setIdViaReflection(entity, 123L);

            // Act - call multiple times
            int hash1 = entity.hashCode();
            int hash2 = entity.hashCode();
            int hash3 = entity.hashCode();

            // Assert
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash2).isEqualTo(hash3);
        }

        @Test
        @DisplayName("should return same hash code regardless of ID value")
        void shouldReturnSameHashCodeRegardlessOfId() throws Exception {
            // Arrange
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            setIdViaReflection(entity1, 1L);
            setIdViaReflection(entity2, 999L);

            // Act & Assert - hash code is based on class, not ID
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }
    }

    @Nested
    @DisplayName("getId")
    class GetId {

        @Test
        @DisplayName("should return the ID when set")
        void shouldReturnIdWhenSet() throws Exception {
            // Arrange
            TestEntity entity = new TestEntity();
            setIdViaReflection(entity, 12345L);

            // Act & Assert
            assertThat(entity.getId()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("should return null when ID is not set")
        void shouldReturnNullWhenIdNotSet() {
            // Arrange
            TestEntity entity = new TestEntity();

            // Act & Assert
            assertThat(entity.getId()).isNull();
        }
    }
}
