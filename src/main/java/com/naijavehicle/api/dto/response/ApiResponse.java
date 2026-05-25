package com.naijavehicle.api.dto.response;

import com.naijavehicle.api.dto.AppConstant;
import com.naijavehicle.api.dto.JwtAuthenticationResponse;
import com.naijavehicle.api.enums.ResponseEnum;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiResponse {

    public static ResponseEntity<ApiResponseBuilder<?>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).
                body(
                        ApiResponseBuilder.builder()
                                .status(ResponseEnum.FAILED.name())
                                .code(ResponseEnum.FAILED.code)
                                .message(message)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<?>>
    created(JwtAuthenticationResponse data) {
        return ResponseEntity.status(HttpStatus.CREATED).
                body(
                        ApiResponseBuilder.builder()
                                .status(ResponseEnum.SUCCESS.name())
                                .code(ResponseEnum.SUCCESS.code)
                                .data(data)
                                .message(AppConstant.CREATED_SUCCESSFUL)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>>
    tooManyRequests() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).
                body(
                        ApiResponseBuilder.<JwtAuthenticationResponse>builder()
                                .status(ResponseEnum.FAILED.name())
                                .code(ResponseEnum.FAILED.code)
                                .message(AppConstant.CREATED_SUCCESSFUL)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>>
    loginSuccess(String message, JwtAuthenticationResponse data) {
        return ResponseEntity.status(HttpStatus.OK).
                body(
                        ApiResponseBuilder.<JwtAuthenticationResponse>builder()
                                .status(ResponseEnum.SUCCESS.name())
                                .code(ResponseEnum.SUCCESS.code)
                                .data(data)
                                .message(message)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>>
    failure(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).
                body(
                        ApiResponseBuilder.<JwtAuthenticationResponse>builder()
                                .status(ResponseEnum.FAILED.name())
                                .code(ResponseEnum.FAILED.code)
                                .message(message)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>>
    success(String message) {
        return ResponseEntity.status(HttpStatus.OK).
                body(
                        ApiResponseBuilder.<JwtAuthenticationResponse>builder()
                                .status(ResponseEnum.SUCCESS.name())
                                .code(ResponseEnum.SUCCESS.code)
                                .message(message)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>>
    success(String message, JwtAuthenticationResponse data) {
        return ResponseEntity.status(HttpStatus.OK).
                body(
                        ApiResponseBuilder.<JwtAuthenticationResponse>builder()
                                .status(ResponseEnum.SUCCESS.name())
                                .code(ResponseEnum.SUCCESS.code)
                                .message(message)
                                .data(data)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder>
    badRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                body(
                        ApiResponseBuilder.builder()
                                .status(ResponseEnum.FAILED.name())
                                .code(ResponseEnum.FAILED.code)
                                .message(AppConstant.INVALID_PAYLOAD)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>>
    badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                body(
                        ApiResponseBuilder.<JwtAuthenticationResponse>builder()
                                .status(ResponseEnum.FAILED.name())
                                .code(ResponseEnum.FAILED.code)
                                .message(message)
                                .build());
    }

    public static ResponseEntity<ApiResponseBuilder<JwtAuthenticationResponse>>
    serverError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).
                body(
                        ApiResponseBuilder.<JwtAuthenticationResponse>builder()
                                .status(ResponseEnum.FAILED.name())
                                .code(ResponseEnum.FAILED.code)
                                .message(message)
                                .build());
    }

}
