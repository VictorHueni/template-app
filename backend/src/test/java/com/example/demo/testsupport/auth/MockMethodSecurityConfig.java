package com.example.demo.testsupport.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Integration-test-only method security configuration.
 *
 * <p>Production enables method security via {@code WebSecurityConfig} with {@code @Profile("!test")}.
 * Integration tests typically run with {@code @ActiveProfiles({"test", "integration"})}, which would
 * disable production method security. This config re-enables it for the {@code integration} profile.
 */
@TestConfiguration
@Profile("integration")
@EnableMethodSecurity
public class MockMethodSecurityConfig {
}
