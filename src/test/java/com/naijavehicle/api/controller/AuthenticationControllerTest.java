package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.*;
import com.naijavehicle.api.models.User;
import com.naijavehicle.api.repositoryService.LoginDetailsRepository;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.security.JwtTokenProvider;
import com.naijavehicle.api.service.GoogleAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private LoginDetailsRepository loginDetailsRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private GoogleAuthService googleAuthService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setBiometricToken("encodedBiometricToken");
        testUser.setUserId("test-user-id");
        // tokenVersion defaults to 0 (int primitive default)
    }

    // -------------------------------------------------------------------------
    // LOGIN
    // -------------------------------------------------------------------------

    @Test
    void testLoginSuccess() {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        when(loginDetailsRepository.countByUsernameAndStatusAndTimestampAfter(anyString(), anyString(), any())).thenReturn(0L);
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken("testuser", 0)).thenReturn("mockAccessToken");
        when(tokenProvider.generateRefreshToken("testuser")).thenReturn("mockRefreshToken");
        when(passwordEncoder.encode(any())).thenReturn("encodedBiometricToken");

        ResponseEntity<?> response = authenticationController.login(loginRequest, mockRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(loginDetailsRepository, times(1)).save(any());
    }

    @Test
    void testLoginFails_BadCredentials() {
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        when(loginDetailsRepository.countByUsernameAndStatusAndTimestampAfter(anyString(), anyString(), any())).thenReturn(0L);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<?> response = authenticationController.login(loginRequest, mockRequest);

        assertEquals(401, response.getStatusCode().value());
        verify(loginDetailsRepository, times(1)).save(any());
    }

    @Test
    void testLoginRateLimited_After5FailedAttempts() {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        when(loginDetailsRepository.countByUsernameAndStatusAndTimestampAfter(anyString(), anyString(), any())).thenReturn(5L);

        ResponseEntity<?> response = authenticationController.login(loginRequest, mockRequest);

        assertEquals(429, response.getStatusCode().value());
        verify(authenticationManager, never()).authenticate(any());
    }

    // -------------------------------------------------------------------------
    // REFRESH
    // -------------------------------------------------------------------------

    @Test
    void testRefreshSuccess() {
        when(tokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("valid-refresh-token")).thenReturn("refresh");
        when(tokenProvider.getUsernameFromToken("valid-refresh-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken("testuser", 0)).thenReturn("newAccessToken");

        ResponseEntity<?> response = authenticationController.refresh("Bearer valid-refresh-token");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRefreshRejected_WhenAccessTokenUsed() {
        when(tokenProvider.validateToken("access-token")).thenReturn(true);
        when(tokenProvider.getTokenType("access-token")).thenReturn("access");

        ResponseEntity<?> response = authenticationController.refresh("Bearer access-token");

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void testRefreshRejected_WhenTokenIsInvalid() {
        when(tokenProvider.validateToken("bad-token")).thenReturn(false);

        ResponseEntity<?> response = authenticationController.refresh("Bearer bad-token");

        assertEquals(401, response.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // CHANGE PASSWORD
    // -------------------------------------------------------------------------

    @Test
    void testChangePasswordSuccess_InvalidatesExistingTokens() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123");
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");

        ResponseEntity<?> response = authenticationController.changePassword(request, authentication);

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository, times(1)).save(testUser);
        assertEquals(1, testUser.getTokenVersion()); // tokenVersion incremented
    }

    @Test
    void testChangePasswordFails_WrongOldPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOld", "newPass123");
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOld", "encodedPassword")).thenReturn(false);

        ResponseEntity<?> response = authenticationController.changePassword(request, authentication);

        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any());
        assertEquals(0, testUser.getTokenVersion()); // tokenVersion unchanged
    }

    // -------------------------------------------------------------------------
    // BIOMETRIC LOGIN
    // -------------------------------------------------------------------------

    @Test
    void testBiometricLoginSuccess() {
        BiometricLoginRequest request = new BiometricLoginRequest("testuser", "rawBiometricToken");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawBiometricToken", "encodedBiometricToken")).thenReturn(true);
        when(tokenProvider.generateToken("testuser", 0)).thenReturn("mockAccessToken");
        when(tokenProvider.generateRefreshToken("testuser")).thenReturn("mockRefreshToken");
        when(passwordEncoder.encode(any())).thenReturn("encodedNewBiometricToken");

        ResponseEntity<?> response = authenticationController.biometricLogin(request, mockRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository, times(1)).save(testUser); // biometric token rotated
    }

    @Test
    void testBiometricLoginFails_InvalidToken() {
        BiometricLoginRequest request = new BiometricLoginRequest("testuser", "wrongToken");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongToken", "encodedBiometricToken")).thenReturn(false);

        ResponseEntity<?> response = authenticationController.biometricLogin(request, mockRequest);

        assertEquals(401, response.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // GOOGLE LOGIN
    // -------------------------------------------------------------------------

    @Test
    void testGoogleLogin_AutoRegistersNewUser() {
        GoogleLoginRequest request = new GoogleLoginRequest("valid-google-id-token");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        GoogleAuthService.GoogleUserInfo googleUser =
                new GoogleAuthService.GoogleUserInfo("google-sub-123", "user@gmail.com", "Test User");

        when(googleAuthService.verifyIdToken("valid-google-id-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("randomEncodedPassword");
        when(userRepository.save(any())).thenReturn(testUser);
        when(tokenProvider.generateToken(any(), anyInt())).thenReturn("mockAccessToken");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("mockRefreshToken");

        ResponseEntity<?> response = authenticationController.googleLogin(request, mockRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository, times(1)).save(any());
        verify(loginDetailsRepository, times(1)).save(any());
    }

    @Test
    void testGoogleLogin_ExistingUserByGoogleId() {
        GoogleLoginRequest request = new GoogleLoginRequest("valid-google-id-token");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        GoogleAuthService.GoogleUserInfo googleUser =
                new GoogleAuthService.GoogleUserInfo("google-sub-123", "user@gmail.com", "Test User");
        testUser.setGoogleId("google-sub-123");

        when(googleAuthService.verifyIdToken("valid-google-id-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(tokenProvider.generateToken(any(), anyInt())).thenReturn("mockAccessToken");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("mockRefreshToken");

        ResponseEntity<?> response = authenticationController.googleLogin(request, mockRequest);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGoogleLogin_LinksExistingAccountByEmail() {
        GoogleLoginRequest request = new GoogleLoginRequest("valid-google-id-token");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        GoogleAuthService.GoogleUserInfo googleUser =
                new GoogleAuthService.GoogleUserInfo("google-sub-123", "testuser@gmail.com", "Test User");
        testUser.setEmail("testuser@gmail.com");
        // testUser.googleId is null — should be linked

        when(googleAuthService.verifyIdToken("valid-google-id-token")).thenReturn(googleUser);
        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("testuser@gmail.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(tokenProvider.generateToken(any(), anyInt())).thenReturn("mockAccessToken");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("mockRefreshToken");

        ResponseEntity<?> response = authenticationController.googleLogin(request, mockRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("google-sub-123", testUser.getGoogleId()); // account linked
    }

    @Test
    void testGoogleLoginFails_InvalidToken() {
        GoogleLoginRequest request = new GoogleLoginRequest("invalid-google-id-token");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        when(googleAuthService.verifyIdToken("invalid-google-id-token")).thenReturn(null);

        ResponseEntity<?> response = authenticationController.googleLogin(request, mockRequest);

        assertEquals(401, response.getStatusCode().value());
        verify(userRepository, never()).findByGoogleId(any());
        verify(userRepository, never()).save(any());
    }
}
