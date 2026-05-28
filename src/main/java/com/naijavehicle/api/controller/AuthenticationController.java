package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.*;
import com.naijavehicle.api.dto.response.ApiResponse;
import com.naijavehicle.api.dto.response.ApiResponseBuilder;
import com.naijavehicle.api.enums.ResponseEnum;
import com.naijavehicle.api.models.LoginDetails;
import com.naijavehicle.api.models.User;
import com.naijavehicle.api.repositoryService.LoginDetailsRepository;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.security.CustomUserDetailsService;
import com.naijavehicle.api.security.JwtTokenProvider;
import com.naijavehicle.api.service.GoogleAuthService;
import com.naijavehicle.api.utils.GeneralUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

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
    private final GoogleAuthService googleAuthService;
    private final CustomUserDetailsService customUserDetailsService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseBuilder<?>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {

        log.info(" the payload -> {}", request);

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ApiResponse.conflict(AppConstant.EMAIL_EXIST);
        }

        User user = User.builder()
                .username(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .ipAddress(GeneralUtils.getIpAddress(httpRequest))
                .appInstallationId(GeneralUtils.getAppInstallationId(httpRequest))
                .roles(Set.of("USER"))
                .build();
        log.info(" the payload -> {}", request);
        userRepository.save(user);

        String accessToken = tokenProvider.generateToken(user.getUsername(), user.getTokenVersion());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

        log.info("New user registered: {}", request.getEmail().replaceAll("[\\r\\n]", ""));

        var data = JwtAuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .appInstallationId(GeneralUtils.getAppInstallationId(httpRequest))
                .build();

        return ApiResponse.created(data);
    }

    @PostMapping("/register/google")
    public ResponseEntity<?> registerWithGoogle(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        log.info("the request -> {}", request);
        GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.verifyIdToken(request.getIdToken());
        if (googleUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Invalid or expired Google token\"}");
        }

        if (userRepository.findByEmail(googleUser.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\": \"Email already registered\"}");
        }

        User user = User.builder()
                .username(googleUser.email())
                .fullName(googleUser.name())
                .email(googleUser.email())
                .googleId(googleUser.sub())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .ipAddress(GeneralUtils.getIpAddress(httpRequest))
                .appInstallationId(GeneralUtils.getAppInstallationId(httpRequest))
                .roles(Set.of("USER"))
                .build();

        userRepository.save(user);

        String accessToken = tokenProvider.generateToken(user.getUsername(), user.getTokenVersion());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

        log.info("New user registered via Google: {}", googleUser.email().replaceAll("[\\r\\n]", ""));

        var result=   JwtAuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .appInstallationId(GeneralUtils.getAppInstallationId(httpRequest))
                .build();
        return ApiResponse.created(result);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                                               HttpServletRequest request) {
        long recentFailures = loginDetailsRepository.countByUsernameAndStatusAndTimestampAfter(
                loginRequest.getUsername(), "FAILURE", LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES));
        if (recentFailures >= MAX_FAILED_ATTEMPTS) {
            return ApiResponse.tooManyRequests();
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );


            User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow(() ->
                    new BadCredentialsException("Invalid user Credentails")
            );

            String rawBiometricToken = UUID.randomUUID().toString();
            user.setBiometricToken(passwordEncoder.encode(rawBiometricToken));
            userRepository.save(user);


            int tokenVersion = user.getTokenVersion();
            String accessToken = tokenProvider.generateToken(authentication.getName(), tokenVersion);
            String refreshToken = tokenProvider.generateRefreshToken(authentication.getName());

            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .biometricToken(rawBiometricToken)
                    .build();

            log.info("User {} logged in successfully", loginRequest.getUsername().replaceAll("[\\r\\n]", ""));

            loginDetailsRepository.save(LoginDetails.builder()
                    .username(loginRequest.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(sanitizeHeader(request.getHeader("location")))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status(ResponseEnum.SUCCESS.name())
                    .build());

            return ApiResponse.loginSuccess(AppConstant.LOGIN_SUCCESS, response);
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername().replaceAll("[\\r\\n]", ""));

            loginDetailsRepository.save(LoginDetails.builder()
                    .username(loginRequest.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(sanitizeHeader(request.getHeader("location")))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status("FAILURE")
                    .failureReason(e.getMessage())
                    .build());

            return ApiResponse.failure(AppConstant.LOGIN_FAILURE);

        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>> refresh(@RequestHeader("Authorization") String authHeader) {
        try {
            log.info("the request header -> {}", authHeader);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ApiResponse.badRequest("Invalid refresh token");
            }

            String refreshToken = authHeader.substring(7);

            if (!tokenProvider.validateToken(refreshToken) ||
                    !"refresh".equals(tokenProvider.getTokenType(refreshToken))) {
                return ApiResponse.failure("Invalid refresh token");

            }
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username).orElse(null);
            int tokenVersion = user != null ? user.getTokenVersion() : 0;
            String newAccessToken = tokenProvider.generateToken(username, tokenVersion);

            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .build();

            log.info("Token refreshed for user: {}", username);
            return ApiResponse.loginSuccess("Token refresh successful", response);
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            return ApiResponse.serverError("An error occurred while refreshing token");
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.failure("user is Unauthorized");
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ApiResponse.badRequest("Invalid old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        return ApiResponse.success("Password changed successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user != null) {
            String resetToken = String.format("%06d", SECURE_RANDOM.nextInt(999999));
            user.setResetToken(passwordEncoder.encode(resetToken));
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            // TODO: In production, send this via Email/SMS. For now, log it.
            log.info("PASSWORD RESET TOKEN FOR {}: {}", user.getUsername(), resetToken);
        }

        return ApiResponse.success("If the username exists, a reset code has been sent.");
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody ResetPasswordConfirmRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null || user.getResetToken() == null) {
            return ApiResponse.badRequest("Invalid or expired reset token");
        }

        if (user.getResetTokenExpiry() != null && LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            return ApiResponse.badRequest("Reset token has expired");
        }

        if (!passwordEncoder.matches(request.getToken(), user.getResetToken())) {
            return ApiResponse.badRequest("Invalid reset token");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return ApiResponse.success("Password has been reset successfully. You can now login.");
    }

    @PostMapping("/login/biometric")
    public ResponseEntity<?> biometricLogin(@Valid @RequestBody BiometricLoginRequest requestBody,
                                                                    HttpServletRequest request) {
        try {
            User user = userRepository.findByUsername(requestBody.getUsername()).orElse(null);

            if (user == null || user.getBiometricToken() == null || !passwordEncoder.matches(requestBody.getBiometricToken(), user.getBiometricToken())) {
                throw new AuthenticationException("Invalid biometric token") {
                };
            }

            String newBiometricToken = UUID.randomUUID().toString();
            user.setBiometricToken(passwordEncoder.encode(newBiometricToken));
            userRepository.save(user);

            String accessToken = tokenProvider.generateToken(user.getUsername(), user.getTokenVersion());
            String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .biometricToken(newBiometricToken)
                    .build();

            log.info("User {} logged in successfully via biometrics", user.getUsername().replaceAll("[\\r\\n]", ""));

            loginDetailsRepository.save(LoginDetails.builder()
                    .username(user.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(sanitizeHeader(request.getHeader("location")))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status("SUCCESS")
                    .build());

            return ApiResponse.success("Biometric login successful", response);
        } catch (Exception e) {
            log.error("Biometric Authentication failed for user: {}", requestBody.getUsername().replaceAll("[\\r\\n]", ""));

            loginDetailsRepository.save(LoginDetails.builder()
                    .username(requestBody.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .location(sanitizeHeader(request.getHeader("location")))
                    .appInstallationId(GeneralUtils.getAppInstallationId(request))
                    .status("FAILURE")
                    .failureReason(e.getMessage())
                    .build());

            return ApiResponse.failure("Biometric login failed");
        }
    }

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponseBuilder<?>> googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                                                 HttpServletRequest httpRequest) {
        GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.verifyIdToken(request.getIdToken());
        if (googleUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            User user = userRepository.findByGoogleId(googleUser.sub()).orElse(null);
            if (user == null) {
                user = userRepository.findByEmail(googleUser.email()).orElse(null);
            }

            if (user == null) {
                user = User.builder()
                        .username(googleUser.email())
                        .fullName(googleUser.name())
                        .email(googleUser.email())
                        .googleId(googleUser.sub())
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .roles(Set.of("USER"))
                        .build();
            } else if (user.getGoogleId() == null) {
                user.setGoogleId(googleUser.sub());
            }
            userRepository.save(user);

            String accessToken = tokenProvider.generateToken(user.getUsername(), user.getTokenVersion());
            String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration)
                    .appInstallationId(GeneralUtils.getAppInstallationId(httpRequest))
                    .build();

            log.info("User {} logged in via Google", googleUser.email());

            loginDetailsRepository.save(LoginDetails.builder()
                    .username(user.getUsername())
                    .ipAddress(GeneralUtils.getIpAddress(httpRequest))
                    .location(sanitizeHeader(httpRequest.getHeader("location")))
                    .appInstallationId(GeneralUtils.getAppInstallationId(httpRequest))
                    .status("SUCCESS")
                    .build());

            return ResponseEntity.ok(
                    ApiResponseBuilder.builder()
                            .code(ResponseEnum.SUCCESS.code)
                            .status(ResponseEnum.SUCCESS.name())
                            .message("Google login successful")
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            log.error("Google login failed for {}: {}", googleUser.email(), e.getMessage());

            loginDetailsRepository.save(LoginDetails.builder()
                    .username(googleUser.email())
                    .ipAddress(GeneralUtils.getIpAddress(httpRequest))
                    .location(sanitizeHeader(httpRequest.getHeader("location")))
                    .appInstallationId(GeneralUtils.getAppInstallationId(httpRequest))
                    .status("FAILURE")
                    .failureReason(e.getMessage())
                    .build());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String sanitizeHeader(String value) {
        if (value == null) return null;
        String sanitized = value.replaceAll("[\\r\\n]", "");
        return sanitized.length() > 200 ? sanitized.substring(0, 200) : sanitized;
    }
}
