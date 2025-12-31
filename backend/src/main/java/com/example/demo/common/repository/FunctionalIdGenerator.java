package com.example.demo.common.repository;

import java.time.Year;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Utility to generate business-readable IDs (Functional IDs).
 * * STRATEGY: Database Sequences
 * * Why:
 * - Atomic: Postgres handles concurrency perfectly.
 * - Non-Blocking: We use REQUIRES_NEW to ensure ID generation doesn't lock the main transaction.
 */
@Component
@RequiredArgsConstructor
public class FunctionalIdGenerator {

    private final EntityManager entityManager;

    /**
     * Generates a formatted ID: PREFIX-YEAR-SEQUENCE
     * e.g., "ORD-2024-000542"
     *
     * @param sequenceName The name of the postgres sequence to use (must exist in DB)
     * @param prefix       The business prefix (e.g., "ORD", "INV", "CUST")
     * @return The formatted identifier string
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(String sequenceName, String prefix) {

        // extremely fast and concurrency-safe
        Number nextVal = (Number) entityManager
                .createNativeQuery("SELECT nextval(:seq)")
                .setParameter("seq", sequenceName)
                .getSingleResult();

        int year = Year.now().getValue();

        // Format: PREFIX-YEAR-SEQUENCE (padded to 6 digits)
        return String.format("%s-%d-%06d", prefix, year, nextVal.longValue());
    }
}