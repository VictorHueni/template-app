package com.example.demo.greeting.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.demo.api.v1.model.GreetingPage;
import com.example.demo.api.v1.model.GreetingResponse;
import com.example.demo.greeting.model.Greeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GreetingMapper.
 *
 * <p>Tests MapStruct-generated implementation against requirements:</p>
 * <ul>
 *   <li>Type conversions (Long→String, Instant→OffsetDateTime)</li>
 *   <li>Null handling</li>
 *   <li>Timezone correctness (UTC)</li>
 *   <li>Page mapping</li>
 * </ul>
 */
@SpringBootTest(classes = {GreetingMapperImpl.class})
class GreetingMapperTest {

    @Autowired
    private GreetingMapper mapper;

    /**
     * Creates a mocked Greeting entity with all required fields.
     */
    private Greeting mockGreeting(Long id, String reference, String message, String recipient, Instant createdAt) {
        Greeting greeting = mock(Greeting.class);
        when(greeting.getId()).thenReturn(id);
        when(greeting.getReference()).thenReturn(reference);
        when(greeting.getMessage()).thenReturn(message);
        when(greeting.getRecipient()).thenReturn(recipient);
        when(greeting.getCreatedAt()).thenReturn(createdAt);
        return greeting;
    }

    @Nested
    @DisplayName("toGreetingResponse")
    class ToGreetingResponse {

        @Test
        @DisplayName("maps all fields correctly")
        void mapsAllFieldsCorrectly() {
            // Arrange
            Long id = 506979954615549952L;
            String reference = "GRE-2025-000042";
            String message = "Hello, World!";
            String recipient = "Alice";
            Instant createdAt = Instant.parse("2025-01-15T12:30:45Z");

            Greeting entity = mockGreeting(id, reference, message, recipient, createdAt);

            // Act
            GreetingResponse dto = mapper.toGreetingResponse(entity);

            // Assert
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo("506979954615549952"); // Long → String
            assertThat(dto.getReference()).isEqualTo(reference);
            assertThat(dto.getMessage()).isEqualTo(message);
            assertThat(dto.getRecipient()).isEqualTo(recipient);
            assertThat(dto.getCreatedAt()).isEqualTo(
                    OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC)
            );
        }

        @Test
        @DisplayName("handles null recipient gracefully")
        void handlesNullRecipient() {
            // Arrange
            Greeting entity = mockGreeting(
                    1L,
                    "GRE-2025-000001",
                    "Test",
                    null, // nullable field
                    Instant.now()
            );

            // Act
            GreetingResponse dto = mapper.toGreetingResponse(entity);

            // Assert
            assertThat(dto.getRecipient()).isNull();
        }

        @Test
        @DisplayName("converts Instant to OffsetDateTime in UTC")
        void convertsInstantToOffsetDateTimeUtc() {
            // Arrange
            Instant instant = Instant.parse("2025-12-31T23:59:59Z");
            Greeting entity = mockGreeting(1L, "GRE-2025-000001", "Test", "Bob", instant);

            // Act
            GreetingResponse dto = mapper.toGreetingResponse(entity);

            // Assert
            assertThat(dto.getCreatedAt()).isNotNull();
            assertThat(dto.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(dto.getCreatedAt().toInstant()).isEqualTo(instant);
        }

        @Test
        @DisplayName("preserves large TSID values as string")
        void preservesLargeTsidAsString() {
            // Arrange - TSID larger than JS Number.MAX_SAFE_INTEGER (2^53-1)
            Long largeTsid = 9007199254740992L; // 2^53 (unsafe in JS)
            Greeting entity = mockGreeting(largeTsid, "GRE-2025-000001", "Test", "Bob", Instant.now());

            // Act
            GreetingResponse dto = mapper.toGreetingResponse(entity);

            // Assert
            assertThat(dto.getId()).isEqualTo(String.valueOf(largeTsid));
            assertThat(Long.parseLong(dto.getId())).isEqualTo(largeTsid); // Round-trip verification
        }

        @Test
        @DisplayName("handles null entity gracefully")
        void handlesNullEntityGracefully() {
            // Act
            GreetingResponse dto = mapper.toGreetingResponse(null);

            // Assert
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("handles null createdAt gracefully")
        void handlesNullCreatedAtGracefully() {
            // Arrange
            Greeting entity = mockGreeting(1L, "GRE-2025-000001", "Test", "Bob", null);

            // Act
            GreetingResponse dto = mapper.toGreetingResponse(entity);

            // Assert
            assertThat(dto.getCreatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("toGreetingPage")
    class ToGreetingPage {

        @Test
        @DisplayName("maps Spring Data Page to GreetingPage DTO")
        void mapsPageToDto() {
            // Arrange
            Greeting entity1 = mockGreeting(1L, "GRE-2025-000001", "Hello", "Alice", Instant.now());
            Greeting entity2 = mockGreeting(2L, "GRE-2025-000002", "World", "Bob", Instant.now());

            Page<Greeting> entityPage = new PageImpl<>(
                    List.of(entity1, entity2),
                    PageRequest.of(0, 10),
                    25 // total elements
            );

            // Act
            GreetingPage dto = mapper.toGreetingPage(entityPage);

            // Assert
            assertThat(dto).isNotNull();
            assertThat(dto.getData()).hasSize(2);
            assertThat(dto.getData().get(0).getId()).isEqualTo("1");
            assertThat(dto.getData().get(1).getId()).isEqualTo("2");

            assertThat(dto.getMeta().getPageNumber()).isEqualTo(0);
            assertThat(dto.getMeta().getPageSize()).isEqualTo(10);
            assertThat(dto.getMeta().getTotalElements()).isEqualTo(25);
            assertThat(dto.getMeta().getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("handles empty page")
        void handlesEmptyPage() {
            // Arrange
            Page<Greeting> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            // Act
            GreetingPage dto = mapper.toGreetingPage(emptyPage);

            // Assert
            assertThat(dto.getData()).isEmpty();
            assertThat(dto.getMeta().getTotalElements()).isZero();
        }

        @Test
        @DisplayName("correctly calculates page metadata")
        void correctlyCalculatesPageMetadata() {
            // Arrange - Page 2 (0-indexed: page 1) of 3 total pages
            Greeting entity = mockGreeting(11L, "GRE-2025-000011", "Test", "User", Instant.now());
            Page<Greeting> page = new PageImpl<>(
                    List.of(entity),
                    PageRequest.of(1, 10), // page 1 (second page)
                    25 // total elements
            );

            // Act
            GreetingPage dto = mapper.toGreetingPage(page);

            // Assert
            assertThat(dto.getMeta().getPageNumber()).isEqualTo(1);
            assertThat(dto.getMeta().getPageSize()).isEqualTo(10);
            assertThat(dto.getMeta().getTotalElements()).isEqualTo(25);
            assertThat(dto.getMeta().getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Type Conversion Qualifiers")
    class TypeConversionQualifiers {

        @Test
        @DisplayName("idToString converts Long to String")
        void idToStringConvertsLongToString() {
            assertThat(mapper.idToString(123L)).isEqualTo("123");
            assertThat(mapper.idToString(null)).isNull();
        }

        @Test
        @DisplayName("stringToId converts String to Long")
        void stringToIdConvertsStringToLong() {
            assertThat(mapper.stringToId("456")).isEqualTo(456L);
            assertThat(mapper.stringToId(null)).isNull();
        }

        @Test
        @DisplayName("idToString and stringToId round-trip correctly")
        void idConversionRoundTrip() {
            Long original = 9876543210L;
            String asString = mapper.idToString(original);
            Long backToLong = mapper.stringToId(asString);

            assertThat(backToLong).isEqualTo(original);
        }

        @Test
        @DisplayName("instantToOffsetDateTime converts to UTC")
        void instantToOffsetDateTimeConvertsToUtc() {
            Instant instant = Instant.parse("2025-06-15T10:30:00Z");
            OffsetDateTime result = mapper.instantToOffsetDateTime(instant);

            assertThat(result).isNotNull();
            assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(result.toInstant()).isEqualTo(instant);
            assertThat(mapper.instantToOffsetDateTime(null)).isNull();
        }

        @Test
        @DisplayName("offsetDateTimeToInstant handles any timezone")
        void offsetDateTimeToInstantHandlesAnyTimezone() {
            // Create OffsetDateTime in different timezone (+05:00)
            OffsetDateTime odt = OffsetDateTime.parse("2025-06-15T15:30:00+05:00");
            Instant result = mapper.offsetDateTimeToInstant(odt);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(Instant.parse("2025-06-15T10:30:00Z")); // Converted to UTC
            assertThat(mapper.offsetDateTimeToInstant(null)).isNull();
        }

        @Test
        @DisplayName("instant and offsetDateTime conversion round-trip correctly")
        void timestampConversionRoundTrip() {
            Instant original = Instant.parse("2025-01-15T12:00:00Z");
            OffsetDateTime asOffsetDateTime = mapper.instantToOffsetDateTime(original);
            Instant backToInstant = mapper.offsetDateTimeToInstant(asOffsetDateTime);

            assertThat(backToInstant).isEqualTo(original);
        }

        @Test
        @DisplayName("handles extreme TSID values")
        void handlesExtremeTsidValues() {
            Long maxLong = Long.MAX_VALUE; // 9223372036854775807
            Greeting entity = mockGreeting(maxLong, "GRE-2025-000001", "Test", "Bob", Instant.now());

            GreetingResponse dto = mapper.toGreetingResponse(entity);

            assertThat(dto.getId()).isEqualTo("9223372036854775807");
            assertThat(Long.parseLong(dto.getId())).isEqualTo(maxLong);
        }

        @Test
        @DisplayName("handles minimum TSID values")
        void handlesMinimumTsidValues() {
            Long minLong = 1L;
            Greeting entity = mockGreeting(minLong, "GRE-2025-000001", "Test", "Bob", Instant.now());

            GreetingResponse dto = mapper.toGreetingResponse(entity);

            assertThat(dto.getId()).isEqualTo("1");
            assertThat(Long.parseLong(dto.getId())).isEqualTo(minLong);
        }

        @Test
        @DisplayName("handles epoch instant")
        void handlesEpochInstant() {
            Instant epoch = Instant.EPOCH; // 1970-01-01T00:00:00Z
            OffsetDateTime result = mapper.instantToOffsetDateTime(epoch);

            assertThat(result).isNotNull();
            assertThat(result.toInstant()).isEqualTo(epoch);
            assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
        }

        @Test
        @DisplayName("handles far future instant")
        void handlesFarFutureInstant() {
            Instant farFuture = Instant.parse("2999-12-31T23:59:59Z");
            OffsetDateTime result = mapper.instantToOffsetDateTime(farFuture);

            assertThat(result).isNotNull();
            assertThat(result.toInstant()).isEqualTo(farFuture);
            assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
        }
    }
}
