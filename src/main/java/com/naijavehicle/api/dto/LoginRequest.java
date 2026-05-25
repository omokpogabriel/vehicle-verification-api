package com.naijavehicle.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Email(message = "Enter a valid email")
    @Pattern(regexp = "\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,6}\\b", message = "Enter a valid email")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}

