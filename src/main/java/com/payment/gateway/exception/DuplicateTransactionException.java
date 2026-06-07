package com.payment.gateway.exception;

public class DuplicateTransactionException extends RuntimeException {

    private final String idempotencyKey;

    public DuplicateTransactionException(String idempotencyKey) {
        super("Transaction already exists for idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}