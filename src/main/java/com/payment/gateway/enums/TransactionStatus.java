package com.payment.gateway.enums;

public enum TransactionStatus {
    INITIATED,      // Payment request received
    FRAUD_CHECK,    // Being checked by fraud engine
    AUTHORIZED,     // Bank approved the payment
    CAPTURED,       // Money actually collected
    SETTLED,        // End of day settlement done
    DECLINED,       // Bank or fraud engine rejected it
    VOIDED,         // Merchant cancelled before capture
    REFUNDED        // Money returned to customer
}