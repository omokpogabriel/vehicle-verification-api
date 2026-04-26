package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.ApiResponse;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.ResponseEnum;
import com.naijavehicle.api.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;

@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/plate/{plateNumber}")
    @Cacheable(value = "verifications", key = "#plateNumber")
    public ResponseEntity<ApiResponse<?>> verifyPlate(@PathVariable String plateNumber,
                                                               HttpServletRequest request
    ) {
            var result = verificationService.verifyPlate(plateNumber, request);
            return ResponseEntity.status(HttpStatus.OK).body(
                 ApiResponse.builder()
                         .status(ResponseEnum.SUCCESS.name())
                         .message("Verification successful")
                         .data(result)
                         .build()
            );

    }


}
