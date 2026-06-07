package com.payment.gateway.exception;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("Invalid or missing API key");
    }

    public InvalidApiKeyException(String message) {
        super(message);
    }
}