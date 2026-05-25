package com.naijavehicle.api.dto.response;

import lombok.Builder;

@Builder
public record ApiResponseBuilder<T>(
        String code,
        String status,
        T data,
        String message
) {}
