package com.example.demo.greeting.dto;

import java.time.Instant;

/**
 * DTO representing a single revision of a Greeting entity.
 *
 * <p>Contains both revision metadata and the entity state at that revision:</p>
 * <ul>
 *   <li>Revision info: number, timestamp, type (INSERT/UPDATE/DELETE), who made the change</li>
 *   <li>Entity state: the Greeting fields as they were at this revision</li>
 * </ul>
 *
 * @param revisionNumber Unique revision identifier
 * @param revisionDate When this revision was created
 * @param revisionType Type of change: INSERT, UPDATE, or DELETE
 * @param modifiedBy Username of who made this change
 * @param id Entity ID (TSID)
 * @param reference Human-readable functional ID
 * @param recipient Greeting recipient
 * @param message Greeting message
 */
public record GreetingRevisionDTO(
        Integer revisionNumber,
        Instant revisionDate,
        String revisionType,
        String modifiedBy,
        Long id,
        String reference,
        String recipient,
        String message
) {

}
