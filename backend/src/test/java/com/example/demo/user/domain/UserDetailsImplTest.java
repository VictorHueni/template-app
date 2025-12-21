package com.example.demo.user.domain;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserDetailsImpl")
class UserDetailsImplTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create user details from username and password")
        void shouldCreateUserDetailsFromUsernameAndPassword() {
            // Arrange
            String username = "testuser";
            String password = "testpass";

            // Act
            UserDetailsImpl user = new UserDetailsImpl(username, password);

            // Assert
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(username);
            assertThat(user.getPassword()).isEqualTo(password);
        }

        @Test
        @DisplayName("should create user with default constructor")
        void shouldCreateUserWithDefaultConstructor() {
            // Act
            UserDetailsImpl user = new UserDetailsImpl();

            // Assert
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isNull();
            assertThat(user.getPassword()).isNull();
        }
    }

    @Nested
    @DisplayName("getUsername")
    class GetUsername {

        @Test
        @DisplayName("should return username")
        void shouldReturnUsername() {
            // Arrange
            String username = "john.doe";
            UserDetailsImpl user = new UserDetailsImpl(username, "pass");

            // Act
            String result = user.getUsername();

            // Assert
            assertThat(result).isEqualTo(username);
        }

        @Test
        @DisplayName("should return updated username after setter")
        void shouldReturnUpdatedUsernameAfterSetter() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("oldname", "pass");

            // Act
            user.setUsername("newname");

            // Assert
            assertThat(user.getUsername()).isEqualTo("newname");
        }
    }

    @Nested
    @DisplayName("getPassword")
    class GetPassword {

        @Test
        @DisplayName("should return password")
        void shouldReturnPassword() {
            // Arrange
            String password = "securePassword123";
            UserDetailsImpl user = new UserDetailsImpl("user", password);

            // Act
            String result = user.getPassword();

            // Assert
            assertThat(result).isEqualTo(password);
        }

        @Test
        @DisplayName("should return updated password after setter")
        void shouldReturnUpdatedPasswordAfterSetter() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("user", "oldpass");

            // Act
            user.setPassword("newpass");

            // Assert
            assertThat(user.getPassword()).isEqualTo("newpass");
        }
    }

    @Nested
    @DisplayName("getAuthorities")
    class GetAuthorities {

        @Test
        @DisplayName("should return empty authorities collection")
        void shouldReturnEmptyAuthoritiesCollection() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("user", "pass");

            // Act
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            // Assert
            assertThat(authorities).isNotNull();
            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("should always return empty list for any user")
        void shouldAlwaysReturnEmptyListForAnyUser() {
            // Arrange
            UserDetailsImpl user1 = new UserDetailsImpl("user1", "pass1");
            UserDetailsImpl user2 = new UserDetailsImpl("user2", "pass2");

            // Act
            Collection<? extends GrantedAuthority> authorities1 = user1.getAuthorities();
            Collection<? extends GrantedAuthority> authorities2 = user2.getAuthorities();

            // Assert
            assertThat(authorities1).isEmpty();
            assertThat(authorities2).isEmpty();
        }
    }

    @Nested
    @DisplayName("isAccountNonExpired")
    class IsAccountNonExpired {

        @Test
        @DisplayName("should always return true")
        void shouldAlwaysReturnTrue() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("user", "pass");

            // Act
            boolean result = user.isAccountNonExpired();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for any user")
        void shouldReturnTrueForAnyUser() {
            // Arrange
            UserDetailsImpl user1 = new UserDetailsImpl("user1", "pass1");
            UserDetailsImpl user2 = new UserDetailsImpl("user2", "pass2");

            // Act & Assert
            assertThat(user1.isAccountNonExpired()).isTrue();
            assertThat(user2.isAccountNonExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("isAccountNonLocked")
    class IsAccountNonLocked {

        @Test
        @DisplayName("should always return true")
        void shouldAlwaysReturnTrue() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("user", "pass");

            // Act
            boolean result = user.isAccountNonLocked();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for any user")
        void shouldReturnTrueForAnyUser() {
            // Arrange
            UserDetailsImpl user1 = new UserDetailsImpl("user1", "pass1");
            UserDetailsImpl user2 = new UserDetailsImpl("user2", "pass2");

            // Act & Assert
            assertThat(user1.isAccountNonLocked()).isTrue();
            assertThat(user2.isAccountNonLocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("isCredentialsNonExpired")
    class IsCredentialsNonExpired {

        @Test
        @DisplayName("should always return true")
        void shouldAlwaysReturnTrue() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("user", "pass");

            // Act
            boolean result = user.isCredentialsNonExpired();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for any user")
        void shouldReturnTrueForAnyUser() {
            // Arrange
            UserDetailsImpl user1 = new UserDetailsImpl("user1", "pass1");
            UserDetailsImpl user2 = new UserDetailsImpl("user2", "pass2");

            // Act & Assert
            assertThat(user1.isCredentialsNonExpired()).isTrue();
            assertThat(user2.isCredentialsNonExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Test
        @DisplayName("should always return true")
        void shouldAlwaysReturnTrue() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("user", "pass");

            // Act
            boolean result = user.isEnabled();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for any user")
        void shouldReturnTrueForAnyUser() {
            // Arrange
            UserDetailsImpl user1 = new UserDetailsImpl("user1", "pass1");
            UserDetailsImpl user2 = new UserDetailsImpl("user2", "pass2");

            // Act & Assert
            assertThat(user1.isEnabled()).isTrue();
            assertThat(user2.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("getId")
    class GetId {

        @Test
        @DisplayName("should return null for unsaved entity")
        void shouldReturnNullForUnsavedEntity() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("user", "pass");

            // Act
            Long id = user.getId();

            // Assert
            assertThat(id).isNull();
        }
    }

    @Nested
    @DisplayName("UserDetails interface implementation")
    class UserDetailsInterfaceImplementation {

        @Test
        @DisplayName("should implement all UserDetails methods")
        void shouldImplementAllUserDetailsMethods() {
            // Arrange
            UserDetailsImpl user = new UserDetailsImpl("testuser", "testpass");

            // Act & Assert - verify all UserDetails methods are callable
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getPassword()).isEqualTo("testpass");
            assertThat(user.getAuthorities()).isEmpty();
            assertThat(user.isAccountNonExpired()).isTrue();
            assertThat(user.isAccountNonLocked()).isTrue();
            assertThat(user.isCredentialsNonExpired()).isTrue();
            assertThat(user.isEnabled()).isTrue();
        }
    }
}
