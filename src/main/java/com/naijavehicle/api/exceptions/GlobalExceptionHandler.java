package com.naijavehicle.api.exceptions;

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
                .body(Map.of(
                        "status", "error",
                        "message", "One or more verification services timed out. Please try again."
                ));
    }

    // Handles validation errors (e.g., if plateNumber is missing)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", "error",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(SystemMalFunctionException.class)
    public ResponseEntity<Object> handleBadRequest(SystemMalFunctionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", "error",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", "error",
                        "message", "An unexpected error occurred."
                ));
    }
}
