package com.example.demo.greeting.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.history.Revision;

import com.example.demo.api.v1.model.GreetingPage;
import com.example.demo.api.v1.model.GreetingResponse;
import com.example.demo.api.v1.model.PageMeta;
import com.example.demo.common.audit.CustomRevisionEntity;
import com.example.demo.greeting.dto.GreetingRevisionDTO;
import com.example.demo.greeting.model.Greeting;

/**
 * MapStruct mapper for Greeting entity and API DTOs.
 *
 * <p>Handles type conversions:</p>
 * <ul>
 *   <li>Long (entity.id) ↔ String (DTO.id) - preserves precision for JavaScript</li>
 *   <li>Instant (entity.createdAt) → OffsetDateTime (DTO.createdAt) - UTC timezone</li>
 * </ul>
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>componentModel = "spring" - Generates @Component for Spring DI</li>
 *   <li>unmappedTargetPolicy = ERROR - Compile error if any field unmapped (safety)</li>
 *   <li>uses = {} - No external mappers needed, uses custom qualifiers</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface GreetingMapper {

    // ========================================
    // Entity → DTO Mappings
    // ========================================

    /**
     * Maps Greeting entity to GreetingResponse DTO.
     *
     * <p>Type conversions:</p>
     * <ul>
     *   <li>id: Long → String (via idToString)</li>
     *   <li>createdAt: Instant → OffsetDateTime UTC (via instantToOffsetDateTime)</li>
     *   <li>reference, message, recipient: Direct mapping</li>
     * </ul>
     *
     * @param greeting JPA entity (never null in practice due to controller logic)
     * @return DTO for API response
     */
    @Mapping(source = "id", target = "id", qualifiedByName = "idToString")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToOffsetDateTime")
    @Mapping(source = "reference", target = "reference")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "recipient", target = "recipient")
    GreetingResponse toGreetingResponse(Greeting greeting);

    /**
     * Maps a Spring Data Page of Greeting entities to GreetingPage DTO.
     *
     * <p>Delegates entity mapping to {@link #toGreetingResponse(Greeting)}.</p>
     *
     * @param page Spring Data Page object
     * @return GreetingPage with mapped content and metadata
     */
    default GreetingPage toGreetingPage(org.springframework.data.domain.Page<Greeting> page) {
        var dtos = page.getContent().stream()
                .map(this::toGreetingResponse)
                .toList();

        // PageMeta constructor: pageNumber, pageSize, totalElements, totalPages
        var meta = new PageMeta(
                page.getNumber(),      // pageNumber
                page.getSize(),        // pageSize
                (int) page.getTotalElements(),  // totalElements
                page.getTotalPages()   // totalPages
        );

        return new GreetingPage(dtos, meta);
    }

    /**
     * Maps Revision&lt;Integer, Greeting&gt; to GreetingRevisionDTO.
     *
     * <p>Extracts revision metadata (number, timestamp, type, user) and entity state.</p>
     *
     * @param revision Hibernate Envers revision wrapper
     * @return DTO with revision metadata and entity snapshot
     */
    default GreetingRevisionDTO toGreetingRevisionDTO(Revision<Integer, Greeting> revision) {
        Greeting entity = revision.getEntity();
        var metadata = revision.getMetadata();

        // Extract username from custom revision entity
        String modifiedBy = metadata.getDelegate() instanceof CustomRevisionEntity cre
                ? cre.getUsername()
                : "unknown";

        return new GreetingRevisionDTO(
                metadata.getRequiredRevisionNumber(),
                metadata.getRequiredRevisionInstant(),
                metadata.getRevisionType().name(),
                modifiedBy,
                entity.getId(),
                entity.getReference(),
                entity.getRecipient(),
                entity.getMessage()
        );
    }

    // ========================================
    // Type Conversion Qualifiers
    // ========================================

    /**
     * Converts entity ID (Long) to DTO ID (String).
     *
     * <p>Why String?</p>
     * <ul>
     *   <li>JavaScript Number.MAX_SAFE_INTEGER = 2^53-1</li>
     *   <li>Java Long max = 2^63-1 (can exceed JS precision)</li>
     *   <li>String representation prevents precision loss</li>
     * </ul>
     *
     * @param id TSID (Long) from entity
     * @return String representation, or null if input is null
     */
    @Named("idToString")
    default String idToString(Long id) {
        return id != null ? String.valueOf(id) : null;
    }

    /**
     * Converts String ID back to Long for entity operations.
     *
     * <p>Used when DTO → Entity mapping is needed (rare in read operations).</p>
     *
     * @param id String ID from DTO
     * @return Long ID for entity
     * @throws NumberFormatException if string is not a valid long
     */
    @Named("stringToId")
    default Long stringToId(String id) {
        return id != null ? Long.parseLong(id) : null;
    }

    /**
     * Converts Instant (JPA audit timestamp) to OffsetDateTime (OpenAPI format).
     *
     * <p>Always uses UTC timezone for consistency:</p>
     * <ul>
     *   <li>DB stores Instant (UTC epoch)</li>
     *   <li>API returns OffsetDateTime in UTC (ISO-8601: "2025-01-15T12:00:00Z")</li>
     *   <li>Clients can convert to local timezone as needed</li>
     * </ul>
     *
     * @param instant JPA audit timestamp (createdAt, updatedAt)
     * @return OffsetDateTime in UTC, or null if input is null
     */
    @Named("instantToOffsetDateTime")
    default OffsetDateTime instantToOffsetDateTime(Instant instant) {
        return instant != null ? OffsetDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }

    /**
     * Converts OffsetDateTime (API) back to Instant (JPA).
     *
     * <p>Handles any timezone offset - converts to UTC epoch for storage.</p>
     *
     * @param offsetDateTime API timestamp
     * @return Instant for JPA storage
     */
    @Named("offsetDateTimeToInstant")
    default Instant offsetDateTimeToInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }
}
