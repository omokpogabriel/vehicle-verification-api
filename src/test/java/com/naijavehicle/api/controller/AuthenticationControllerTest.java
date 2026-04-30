package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.BiometricLoginRequest;
import com.naijavehicle.api.dto.ChangePasswordRequest;
import com.naijavehicle.api.dto.LoginRequest;
import com.naijavehicle.api.models.User;
import com.naijavehicle.api.repositoryService.LoginDetailsRepository;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginDetailsRepository loginDetailsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
    }

    @Test
    void testLoginSuccess() {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken("testuser")).thenReturn("mockAccessToken");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(any())).thenReturn("encodedNewToken");

        ResponseEntity<?> response = authenticationController.login(loginRequest, mockRequest);
        
        assertEquals(200, response.getStatusCode().value());
        Mockito.verify(loginDetailsRepository, Mockito.times(1)).save(any());
    }

    @Test
    void testBiometricLoginSuccess() {
        BiometricLoginRequest request = new BiometricLoginRequest("testuser", "rawBiometricToken");
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawBiometricToken", "encodedBiometricToken")).thenReturn(true);
        when(tokenProvider.generateToken("testuser")).thenReturn("mockAccessToken");
        when(passwordEncoder.encode(any())).thenReturn("encodedNewBiometricToken");

        ResponseEntity<?> response = authenticationController.biometricLogin(request, mockRequest);
        assertEquals(200, response.getStatusCode().value());
    }
}
