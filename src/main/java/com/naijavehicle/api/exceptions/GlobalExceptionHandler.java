package com.naijavehicle.api.exceptions;

import com.naijavehicle.api.dto.ApiResponse;
import com.naijavehicle.api.enums.ResponseEnum;
import org.springframework.web.bind.annotation.ControllerAdvice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler({TimeoutException.class, CompletionException.class})
    public ResponseEntity<Object> handleTimeout(Exception ex) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(
                        ApiResponse.builder()
                                .status(ResponseEnum.FAILED.name())
                                .message("One or more verification services timed out. Please try again.")
                                .build()

                );
    }

    // Handles validation errors (e.g., if plateNumber is missing)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .status(ResponseEnum.FAILED.name())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(SystemMalFunctionException.class)
    public ResponseEntity<Object> handleBadRequest(SystemMalFunctionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .status(ResponseEnum.FAILED.name())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                        .status(ResponseEnum.SUCCESS.name())
                        .message("Something went through, we are working on it")
                        .build()
                );
    }
}
