package com.naijavehicle.api.dto;

import com.naijavehicle.api.models.VehicleReport;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Builder
public record VerificationResponseObject(int totalCount, Long successCount,
                                         List<ScrapingResult<?>> report, Instant nextRetry) {
}
