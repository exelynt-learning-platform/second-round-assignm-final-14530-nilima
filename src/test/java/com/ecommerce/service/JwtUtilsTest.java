package com.ecommerce.service;

import com.ecommerce.security.JwtUtils;
import com.ecommerce.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtils Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000);
    }

    @Test
    @DisplayName("Generate and validate token")
    void generateAndValidateToken() {
        String token = jwtUtils.generateTokenFromUsername("testuser");
        assertThat(token).isNotBlank();
        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
    }

    @Test
    @DisplayName("Extract username from token")
    void extractUsernameFromToken() {
        String token = jwtUtils.generateTokenFromUsername("testuser");
        String username = jwtUtils.getUserNameFromJwtToken(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Invalid token fails validation")
    void invalidTokenFailsValidation() {
        assertThat(jwtUtils.validateJwtToken("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("Generate token from Authentication")
    void generateTokenFromAuthentication() {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@test.com", "hashed", List.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of());

        String token = jwtUtils.generateJwtToken(auth);
        assertThat(token).isNotBlank();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Empty token fails validation")
    void emptyTokenFailsValidation() {
        assertThat(jwtUtils.validateJwtToken("")).isFalse();
    }
}
