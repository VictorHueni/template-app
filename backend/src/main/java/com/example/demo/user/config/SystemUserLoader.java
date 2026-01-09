package com.example.demo.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.user.model.UserDetailsImpl;
import com.example.demo.user.repository.UserRepository;

@Configuration
@Profile({"test & !integration", "dev"}) // WARNING: Do not enable in production environments
public class SystemUserLoader {

    @Value("${admin.username:user}")
    private String adminUsername;

    @Value("${admin.password:password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initSystemUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Check if user already exists to prevent startup errors
            if (userRepository.findByUsername(adminUsername).isEmpty()) {

                // 2. Create the user object
                UserDetailsImpl systemUser = new UserDetailsImpl();
                systemUser.setUsername(adminUsername);

                // 3. Encode the password dynamically at runtime
                // TODO : Variabilize the password for better security practices
                systemUser.setPassword(passwordEncoder.encode(adminPassword));

                // 4. Save to DB
                userRepository.save(systemUser);
            }
        };
    }
}
