package com.example.demo.user.service;

import com.example.demo.user.domain.UserDetailsImpl;
import com.example.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should return user details when user exists")
        void shouldReturnUserDetailsWhenUserExists() {
            // Arrange
            String username = "testuser";
            UserDetailsImpl user = new UserDetailsImpl(username, "password123");
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(username);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getPassword()).isEqualTo("password123");
            verify(userRepository).findByUsername(username);
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user does not exist")
        void shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
            // Arrange
            String username = "nonexistent";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findByUsername(username);
        }

        @Test
        @DisplayName("should map user to UserDetails correctly")
        void shouldMapUserToUserDetailsCorrectly() {
            // Arrange
            String username = "john.doe";
            String password = "securePassword";
            UserDetailsImpl user = new UserDetailsImpl(username, password);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(username);

            // Assert
            assertThat(result).isInstanceOf(UserDetailsImpl.class);
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getPassword()).isEqualTo(password);
        }

        @Test
        @DisplayName("should call repository with exact username")
        void shouldCallRepositoryWithExactUsername() {
            // Arrange
            String username = "admin";
            UserDetailsImpl user = new UserDetailsImpl(username, "admin123");
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            userDetailsService.loadUserByUsername(username);

            // Assert
            verify(userRepository).findByUsername(username);
        }

        @Test
        @DisplayName("should handle different usernames correctly")
        void shouldHandleDifferentUsernamesCorrectly() {
            // Arrange
            String username1 = "user1";
            String username2 = "user2";
            UserDetailsImpl user1 = new UserDetailsImpl(username1, "pass1");
            UserDetailsImpl user2 = new UserDetailsImpl(username2, "pass2");

            when(userRepository.findByUsername(username1)).thenReturn(Optional.of(user1));
            when(userRepository.findByUsername(username2)).thenReturn(Optional.of(user2));

            // Act
            UserDetails result1 = userDetailsService.loadUserByUsername(username1);
            UserDetails result2 = userDetailsService.loadUserByUsername(username2);

            // Assert
            assertThat(result1.getUsername()).isEqualTo(username1);
            assertThat(result2.getUsername()).isEqualTo(username2);
        }

        @Test
        @DisplayName("should throw exception with correct message when user not found")
        void shouldThrowExceptionWithCorrectMessage() {
            // Arrange
            String username = "unknown";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create service with user repository")
        void shouldCreateServiceWithUserRepository() {
            // Arrange
            UserRepository repository = userRepository;

            // Act
            UserDetailsServiceImpl service = new UserDetailsServiceImpl(repository);

            // Assert
            assertThat(service).isNotNull();
        }
    }
}
