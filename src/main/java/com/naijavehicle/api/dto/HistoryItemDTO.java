package com.naijavehicle.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naijavehicle.api.enums.ChannelEnum;
import com.naijavehicle.api.models.UserVerification;
import com.naijavehicle.api.models.VehicleReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItemDTO {

    private String reportId;
    private String plateNumber;
    private String make;
    private String model;
    private LocalDateTime verifiedAt;
    private List<CheckItemDTO> checks;

    public static HistoryItemDTO from(VehicleReport report, UserVerification uv, ObjectMapper objectMapper) {
        String make = null;
        String model = null;

        Map<ChannelEnum, ScrapingResult<?>> results = report.getResults();

        if (results != null) {
            ScrapingResult<?> autoReg = results.get(ChannelEnum.AUTO_REG);
            if (autoReg != null && autoReg.getAdditionalInfo() != null) {
                try {
                    VehicleAdditionalInfoDTO info = objectMapper.convertValue(
                            autoReg.getAdditionalInfo(), VehicleAdditionalInfoDTO.class);

                    model = info.getModel();
                } catch (Exception ignored) {}
            }
        }

        List<CheckItemDTO> checks = results == null ? List.of() :
                results.values().stream()
                        .map(r -> new CheckItemDTO(r.getType(), r.getCode(), r.getStatus()))
                        .toList();

        return HistoryItemDTO.builder()
                .reportId(report.getReportId())
                .plateNumber(report.getPlateNumber())
                .make(make)
                .model(model)
                .verifiedAt(uv.getVerifiedAt())
                .checks(checks)
                .build();
    }
}
