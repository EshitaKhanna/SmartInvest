package com.example.investmentplatform.exceptions;

public class InvalidAllocationException extends RuntimeException {
    public InvalidAllocationException(String message) {
        super(message);
    }
}