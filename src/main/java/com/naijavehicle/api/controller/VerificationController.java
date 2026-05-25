package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.response.ApiResponseBuilder;
import com.naijavehicle.api.enums.ResponseEnum;
import com.naijavehicle.api.models.User;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.repositoryService.VehicleReportRepository;
import com.naijavehicle.api.service.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.cache.annotation.Cacheable;

@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final VerificationService verificationService;
    private final VehicleReportRepository vehicleReportRepository;
    private final UserRepository userRepository;

    @GetMapping("/plate/{plateNumber}")
    @Cacheable(value = "verifications", key = "#plateNumber")
    public ResponseEntity<ApiResponseBuilder<?>> verifyPlate(@PathVariable @NotBlank String plateNumber,
                                                             HttpServletRequest request
    ) {
            var result = verificationService.verifyPlate(plateNumber.toUpperCase(), request);
            return ResponseEntity.status(HttpStatus.OK).body(
                 ApiResponseBuilder.builder()
                         .status(ResponseEnum.SUCCESS.name())
                         .message("Verification successful")
                         .data(result)
                         .build()
            );

    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponseBuilder<?>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                 ApiResponseBuilder.builder()
                         .status(ResponseEnum.FAILED.name())
                         .message("User not authenticated")
                         .build()
            );
        }
        
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
            page, 
            size, 
            org.springframework.data.domain.Sort.by("updatedAt").descending()
        );

        var reportsPage = vehicleReportRepository.findByUserId(user.getUserId(), pageable);
        
        return ResponseEntity.status(HttpStatus.OK).body(
             ApiResponseBuilder.builder()
                     .status(ResponseEnum.SUCCESS.name())
                     .message("History fetched successfully")
                     .data(reportsPage)
                     .build()
        );
    }
}
