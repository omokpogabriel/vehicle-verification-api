package com.naijavehicle.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naijavehicle.api.dto.HistoryItemDTO;
import com.naijavehicle.api.dto.VerificationResponseObject;
import com.naijavehicle.api.dto.response.ApiResponseBuilder;
import com.naijavehicle.api.enums.ResponseEnum;
import com.naijavehicle.api.models.User;
import com.naijavehicle.api.models.UserVerification;
import com.naijavehicle.api.models.VehicleReport;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.repositoryService.UserVerificationRepository;
import com.naijavehicle.api.repositoryService.VehicleReportRepository;
import com.naijavehicle.api.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final VerificationService verificationService;
    private final VehicleReportRepository vehicleReportRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/plate/{plateNumber}")
    public ResponseEntity<ApiResponseBuilder<?>> verifyPlate(@PathVariable @NotBlank String plateNumber,
                                                             HttpServletRequest request
    ) {
        var result = verificationService.verifyPlate(plateNumber.toUpperCase(), request);

        Long totalSuccessSearch = result.stream().filter(results -> results.getCode()
                        .equalsIgnoreCase(ResponseEnum.SUCCESS.code))
                .count();

        VerificationResponseObject vro = VerificationResponseObject.builder()
                .totalCount(result.size())
                .nextRetry(Instant.now().plusSeconds(30))
                .successCount(totalSuccessSearch).report(result).build();
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponseBuilder.builder()
                        .status(ResponseEnum.SUCCESS.name())
                        .message("Verification successful")
                        .data(vro)
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
                org.springframework.data.domain.Sort.by("verifiedAt").descending()
        );

        var uvPage = userVerificationRepository.findByUserId(user.getUserId(), pageable);

        List<String> plateNumbers = uvPage.getContent().stream()
                .map(UserVerification::getPlateNumber)
                .toList();

        Map<String, VehicleReport> reportMap = vehicleReportRepository.findByPlateNumbers(plateNumbers)
                .stream()
                .collect(Collectors.toMap(VehicleReport::getPlateNumber, r -> r));

        Map<String, UserVerification> uvMap = uvPage.getContent().stream()
                .collect(Collectors.toMap(UserVerification::getPlateNumber, uv -> uv));

        List<HistoryItemDTO> items = plateNumbers.stream()
                .map(plate -> {
                    VehicleReport report = reportMap.get(plate);
                    UserVerification uv = uvMap.get(plate);
                    return (report != null && uv != null)
                            ? HistoryItemDTO.from(report, uv, objectMapper)
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();

        var reportsPage = new PageImpl<>(items, pageable, uvPage.getTotalElements());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponseBuilder.builder()
                        .status(ResponseEnum.SUCCESS.name())
                        .message("History fetched successfully")
                        .data(reportsPage)
                        .build()
        );
    }


}
