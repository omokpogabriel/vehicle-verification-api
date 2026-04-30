package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.JwtAuthenticationResponse;
import com.naijavehicle.api.dto.LoginRequest;
import com.naijavehicle.api.dto.ChangePasswordRequest;
import com.naijavehicle.api.dto.ResetPasswordRequest;
import com.naijavehicle.api.dto.ResetPasswordConfirmRequest;
import com.naijavehicle.api.dto.BiometricLoginRequest;
import com.naijavehicle.api.models.LoginDetails;
import com.naijavehicle.api.models.User;
import com.naijavehicle.api.repositoryService.LoginDetailsRepository;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.security.JwtTokenProvider;
import com.naijavehicle.api.utils.GeneralUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final LoginDetailsRepository loginDetailsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String accessToken = tokenProvider.generateToken(authentication.getName());
            String refreshToken = tokenProvider.generateRefreshToken(authentication.getName());

            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            
            String rawBiometricToken = UUID.randomUUID().toString();
            if (user != null) {
                user.setBiometricToken(passwordEncoder.encode(rawBiometricToken));
                userRepository.save(user);
            }

            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .biometricToken(rawBiometricToken)
                    .build();

            log.info("User {} logged in successfully", loginRequest.getUsername());
            
            // Track successful login
            loginDetailsRepository.save(LoginDetails.builder()
                    .username(loginRequest.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(request.getHeader("location"))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status("SUCCESS")
                    .build());

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername());
            
            // Track failed login
            loginDetailsRepository.save(LoginDetails.builder()
                    .username(loginRequest.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(request.getHeader("location"))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status("FAILURE")
                    .failureReason(e.getMessage())
                    .build());
                    
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String refreshToken = authHeader.substring(7);

            if (!tokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = tokenProvider.getUsernameFromToken(refreshToken);
            String newAccessToken = tokenProvider.generateToken(username);

            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .build();

            log.info("Token refreshed for user: {}", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("{\"error\": \"Invalid old password\"}");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("{\"message\": \"Password changed successfully\"}");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody ResetPasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user != null) {
            String resetToken = String.format("%06d", new java.util.Random().nextInt(999999)); // 6-digit OTP
            user.setResetToken(passwordEncoder.encode(resetToken));
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            
            // TODO: In production, send this via Email/SMS. For now, log it.
            log.info("PASSWORD RESET TOKEN FOR {}: {}", user.getUsername(), resetToken);
        }
        
        // Always return OK to prevent username enumeration
        return ResponseEntity.ok("{\"message\": \"If the username exists, a reset code has been sent.\"}");
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<?> confirmPasswordReset(@RequestBody ResetPasswordConfirmRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        
        if (user == null || user.getResetToken() == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Invalid or expired reset token\"}");
        }
        
        if (user.getResetTokenExpiry() != null && LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            return ResponseEntity.badRequest().body("{\"error\": \"Reset token has expired\"}");
        }

        if (!passwordEncoder.matches(request.getToken(), user.getResetToken())) {
            return ResponseEntity.badRequest().body("{\"error\": \"Invalid reset token\"}");
        }

        // Token is valid, reset password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("{\"message\": \"Password has been reset successfully. You can now login.\"}");
    }

    @PostMapping("/login/biometric")
    public ResponseEntity<JwtAuthenticationResponse> biometricLogin(@RequestBody BiometricLoginRequest requestBody, HttpServletRequest request) {
        try {
            User user = userRepository.findByUsername(requestBody.getUsername()).orElse(null);
            
            if (user == null || user.getBiometricToken() == null || !passwordEncoder.matches(requestBody.getBiometricToken(), user.getBiometricToken())) {
                throw new AuthenticationException("Invalid biometric token") {};
            }

            // Generate new session tokens
            String accessToken = tokenProvider.generateToken(user.getUsername());
            String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());
            
            // Rotate biometric token for security
            String newBiometricToken = UUID.randomUUID().toString();
            user.setBiometricToken(passwordEncoder.encode(newBiometricToken));
            userRepository.save(user);

            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .biometricToken(newBiometricToken)
                    .build();

            log.info("User {} logged in successfully via biometrics", user.getUsername());
            
            loginDetailsRepository.save(LoginDetails.builder()
                    .username(user.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(request.getHeader("location"))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status("SUCCESS")
                    .build());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Biometric Authentication failed for user: {}", requestBody.getUsername());
            
            loginDetailsRepository.save(LoginDetails.builder()
                    .username(requestBody.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(request.getHeader("location"))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status("FAILURE")
                    .failureReason(e.getMessage())
                    .build());
                    
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}

