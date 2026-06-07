package com.payment.gateway.dto;

import com.payment.gateway.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {

    private UUID transactionId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private String cardLastFour;
    private String cardBrand;
    private String message;
    private LocalDateTime createdAt;
}