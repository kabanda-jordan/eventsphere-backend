package com.eventsphere.security;

import com.eventsphere.entity.User;
import com.eventsphere.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtTokenProvider — covers token generation, validation, and claim extraction.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 256-bit Base64-encoded secret (same as application-test.yml)
    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", 86400000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 604800000L);
        // Manually call @PostConstruct
        jwtTokenProvider.init();
    }

    private User buildUser(String username, Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setFullName("Test User");
        user.setRole(role);
        return user;
    }

    // ─── Test 17: Token generation and username extraction ──────────────────────

    @Test
    @DisplayName("generateToken() — produces a valid JWT with correct subject")
    void generateToken_validUser_extractsCorrectUsername() {
        User user = buildUser("alice", Role.STUDENT);

        String token = jwtTokenProvider.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("alice");
    }

    // ─── Test 18: Token validation — valid token ────────────────────────────────

    @Test
    @DisplayName("validateToken() — returns true for a freshly generated token")
    void validateToken_freshToken_returnsTrue() {
        User user = buildUser("bob", Role.ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    // ─── Test 19: Token validation — tampered token ─────────────────────────────

    @Test
    @DisplayName("validateToken() — returns false for a tampered/invalid token")
    void validateToken_tamperedToken_returnsFalse() {
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.tampered.signature";

        assertThat(jwtTokenProvider.validateToken(fakeToken)).isFalse();
    }

    // ─── Test 20: Role claim extraction ─────────────────────────────────────────

    @Test
    @DisplayName("getRole() — extracts correct role claim from token")
    void getRole_adminUser_returnsAdminRole() {
        User user = buildUser("carol", Role.ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("ADMIN");
    }

    // ─── Test 21: Refresh token is different from access token ──────────────────

    @Test
    @DisplayName("generateRefreshToken() — produces a token different from the access token")
    void generateRefreshToken_isDifferentFromAccessToken() {
        User user = buildUser("dave", Role.STUDENT);

        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        assertThat(refreshToken).isNotEqualTo(accessToken);
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.extractUsername(refreshToken)).isEqualTo("dave");
    }
}
