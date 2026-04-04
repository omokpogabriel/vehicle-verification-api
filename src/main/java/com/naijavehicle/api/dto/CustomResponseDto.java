package com.naijavehicle.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomResponseDto(String status, String message, @JsonAlias("duty_record_found")
boolean dutyRecordFound, Object data) {
}
