package com.example.investmentplatform.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception e) {
        e.printStackTrace();  // For debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Internal Server Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown"));
    }

    // Optional: catch specific errors like JWT or Auth
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                             .body("Access Denied: " + e.getMessage());
    }

}
