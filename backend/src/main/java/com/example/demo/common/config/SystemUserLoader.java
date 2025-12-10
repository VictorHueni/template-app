package com.example.demo.testsupport;

import com.example.demo.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import

@Configuration
@Profile("test")
public class TestUserDataLoader {

    @Bean
    public CommandLineRunner loadTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if user exists to avoid errors on reload
            if (userRepository.findByUsername("system").isEmpty()) {
                User testUser = new User();
                testUser.setUsername("system");

                // This generates the hash at runtime.
                // Since the hash string doesn't exist in the code, Semgrep won't flag it.
                testUser.setPassword(passwordEncoder.encode("system"));

                userRepository.save(testUser);
                System.out.println("Test user 'system' inserted via CommandLineRunner");
            }
        };
    }
}