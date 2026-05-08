package com.naijavehicle.api.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GoogleAuthService.
 * Disabled by default because they make real HTTP calls to Google's tokeninfo endpoint.
 * To run: obtain a valid Google ID token from the Google OAuth2 Playground,
 * set GOOGLE_CLIENT_ID in your environment, then remove @Disabled.
 */
@Disabled("Makes real HTTP calls to Google tokeninfo endpoint - enable manually")
@SpringBootTest
public class GoogleAuthServiceTest {

    @Autowired
    private GoogleAuthService googleAuthService;

    @Test
    void testVerifyIdToken_InvalidToken_ReturnsNull() {
        GoogleAuthService.GoogleUserInfo result = googleAuthService.verifyIdToken("invalid.token.here");
        assertNull(result);
    }

    @Test
    void testVerifyIdToken_ExpiredToken_ReturnsNull() {
        // Replace with a real but expired Google ID token to verify expiry handling
        GoogleAuthService.GoogleUserInfo result = googleAuthService.verifyIdToken("<expired-google-id-token>");
        assertNull(result);
    }

    @Test
    void testVerifyIdToken_ValidToken_ReturnsUserInfo() {
        // Replace with a fresh Google ID token from the Google OAuth2 Playground
        GoogleAuthService.GoogleUserInfo result = googleAuthService.verifyIdToken("<valid-google-id-token>");
        assertNotNull(result);
        assertNotNull(result.sub());
        assertNotNull(result.email());
        assertTrue(result.email().contains("@"));
    }
}
