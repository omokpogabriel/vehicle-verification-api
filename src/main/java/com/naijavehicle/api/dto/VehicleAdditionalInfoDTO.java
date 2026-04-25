package com.naijavehicle.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
public class VehicleAdditionalInfoDTO {
    private String model;
    private String color;
    private String chassis;
    private String service;
    private String expiryDate;


    public static VehicleAdditionalInfoDTO fromRawString(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        // Split by pipe and convert to a Map for easy lookup
        Map<String, String> dataMap = Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> s.contains(":"))
                .map(s -> s.split(":", 2))
                .collect(Collectors.toMap(
                        part -> part[0].trim().toLowerCase(),
                        part -> part[1].trim(),
                        (existing, replacement) -> existing
                ));

        return VehicleAdditionalInfoDTO.builder()
                .model(dataMap.get("vehicle model"))
                .color(dataMap.get("vehicle color"))
                .chassis(dataMap.get("chassis"))
                .service(dataMap.get("service"))
                .expiryDate(dataMap.get("expiry"))
                .build();
    }
}
