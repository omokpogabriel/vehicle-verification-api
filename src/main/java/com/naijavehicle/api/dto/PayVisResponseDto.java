package com.naijavehicle.api.dto;


import java.util.List;

public record PayVisResponseDto(List localBills, List compoundBills, List externalBills) {
}
