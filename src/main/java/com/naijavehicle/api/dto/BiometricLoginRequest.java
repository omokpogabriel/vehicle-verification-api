package com.naijavehicle.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricLoginRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Biometric token is required")
    private String biometricToken;
}
