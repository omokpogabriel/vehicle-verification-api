package com.naijavehicle.api.exceptions;

import com.naijavehicle.api.dto.response.ApiResponseBuilder;
import com.naijavehicle.api.enums.ResponseEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseBuilder.builder()
                        .status(ResponseEnum.FAILED.name())
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler({TimeoutException.class, CompletionException.class})
    public ResponseEntity<Object> handleTimeout(Exception ex) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(
                        ApiResponseBuilder.builder()
                                .status(ResponseEnum.FAILED.name())
                                .message("One or more verification services timed out. Please try again.")
                                .build()

                );
    }

    // Handles validation errors (e.g., if plateNumber is missing)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseBuilder.builder()
                        .status(ResponseEnum.FAILED.name())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(SystemMalFunctionException.class)
    public ResponseEntity<Object> handleBadRequest(SystemMalFunctionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseBuilder.builder()
                        .status(ResponseEnum.FAILED.name())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralError(Exception ex) {
        log.info("An unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseBuilder.builder()
                        .status(ResponseEnum.FAILED.name())
                        .message("Something went wrong, we are working on it")
                        .build()
                );
    }
}
