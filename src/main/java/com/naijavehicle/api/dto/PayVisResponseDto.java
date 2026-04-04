package com.naijavehicle.api.dto;


public record PayVisResponseDto(String[] localBills, String[] compoundBills, String[] externalBills) {
}
