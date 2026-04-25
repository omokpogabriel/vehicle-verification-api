package com.naijavehicle.api.dto;

import lombok.Builder;

@Builder
public record ApiResponse<T>(
        String status,
        T data,
        String message
) {}
