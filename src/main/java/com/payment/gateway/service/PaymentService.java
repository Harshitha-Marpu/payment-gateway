package com.payment.gateway.service;

import com.payment.gateway.dto.FraudCheckResult;
import com.payment.gateway.dto.PaymentRequest;
import com.payment.gateway.dto.PaymentResponse;
import com.payment.gateway.entity.Transaction;
import com.payment.gateway.enums.TransactionStatus;
import com.payment.gateway.exception.TransactionNotFoundException;
import com.payment.gateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.payment.gateway.exception.TransactionNotFoundException;
import com.payment.gateway.dto.FraudCheckResult;
import com.payment.gateway.service.FraudService;



@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final FraudService fraudService;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        // Step 1: Check idempotency — if same request comes twice, return same response
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            log.warn("Duplicate request detected for idempotency key: {}", request.getIdempotencyKey());
            Transaction existing = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElseThrow();
            return mapToResponse(existing, "Duplicate request - returning existing transaction");
        }

        // Step 2: Extract last 4 digits of card (never store full number)
        String cardLastFour = request.getCardNumber()
            .substring(request.getCardNumber().length() - 4);

        // Step 3: Detect card brand from first digit
        String cardBrand = detectCardBrand(request.getCardNumber());

        // Step 4: Build and save the transaction
        Transaction transaction = Transaction.builder()
            .merchantId(request.getMerchantId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(TransactionStatus.INITIATED)
            .idempotencyKey(request.getIdempotencyKey())
            .cardLastFour(cardLastFour)
            .cardBrand(cardBrand)
            .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: {} for merchant: {}", saved.getId(), saved.getMerchantId());

        // Step 5: Move to FRAUD_CHECK state
        saved.setStatus(TransactionStatus.FRAUD_CHECK);
        saved = transactionRepository.save(saved);
        log.info("Transaction {} moved to FRAUD_CHECK", saved.getId());

        // Step 6: Real fraud check
FraudCheckResult fraudResult = fraudService.evaluate(request);
log.info("Fraud check result for {}: {}", saved.getId(), fraudResult.getSummary());

if (fraudResult.isBlocked()) {
    saved.setStatus(TransactionStatus.DECLINED);
    saved.setFailureReason("Blocked by fraud engine. Score: " +
        fraudResult.getRiskScore() + ". Rules: " +
        String.join(", ", fraudResult.getTriggeredRules()));
    transactionRepository.save(saved);
    log.warn("Transaction {} DECLINED — risk score: {}",
        saved.getId(), fraudResult.getRiskScore());
    return mapToResponse(saved, "Payment declined — fraud risk too high");
}

if (fraudResult.needsReview()) {
    log.warn("Transaction {} flagged for REVIEW — risk score: {}",
        saved.getId(), fraudResult.getRiskScore());
    // In Phase 5 we'll send this to a review queue
    // For now we allow it but log it prominently
}
        // Step 7: Move to AUTHORIZED (simulating bank approval for now)
        saved.setStatus(TransactionStatus.AUTHORIZED);
        saved = transactionRepository.save(saved);
        log.info("Transaction {} AUTHORIZED", saved.getId());

        return mapToResponse(saved, "Payment authorized successfully");
    }

    public PaymentResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository
            .findById(java.util.UUID.fromString(transactionId))
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return mapToResponse(transaction, "Transaction found");
    }

    // Detects card brand from first digit
    private String detectCardBrand(String cardNumber) {
        char firstDigit = cardNumber.charAt(0);
        return switch (firstDigit) {
            case '4' -> "VISA";
            case '5' -> "MASTERCARD";
            case '3' -> "AMEX";
            case '6' -> "DISCOVER";
            default  -> "UNKNOWN";
        };
    }
    
    // Converts a Transaction entity to a PaymentResponse DTO
    private PaymentResponse mapToResponse(Transaction transaction, String message) {
        return PaymentResponse.builder()
            .transactionId(transaction.getId())
            .merchantId(transaction.getMerchantId())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .status(transaction.getStatus())
            .cardLastFour(transaction.getCardLastFour())
            .cardBrand(transaction.getCardBrand())
            .message(message)
            .createdAt(transaction.getCreatedAt())
            .build();
    }
    @Transactional
public PaymentResponse capturePayment(String transactionId) {
    Transaction transaction = transactionRepository
        .findById(java.util.UUID.fromString(transactionId))
        .orElseThrow(() -> new TransactionNotFoundException(transactionId));

    if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
        throw new RuntimeException("Transaction cannot be captured. Current status: "
            + transaction.getStatus());
    }

    transaction.setStatus(TransactionStatus.CAPTURED);
    transaction = transactionRepository.save(transaction);
    log.info("Transaction {} CAPTURED", transaction.getId());
    return mapToResponse(transaction, "Payment captured successfully");
}

@Transactional
public PaymentResponse refundPayment(String transactionId) {
    Transaction transaction = transactionRepository
        .findById(java.util.UUID.fromString(transactionId))
        .orElseThrow(() -> new TransactionNotFoundException(transactionId));

    if (transaction.getStatus() != TransactionStatus.CAPTURED
            && transaction.getStatus() != TransactionStatus.SETTLED) {
        throw new RuntimeException("Transaction cannot be refunded. Current status: "
            + transaction.getStatus());
    }

    transaction.setStatus(TransactionStatus.REFUNDED);
    transaction = transactionRepository.save(transaction);
    log.info("Transaction {} REFUNDED", transaction.getId());
    return mapToResponse(transaction, "Payment refunded successfully");
}

@Transactional
public PaymentResponse voidPayment(String transactionId) {
    Transaction transaction = transactionRepository
        .findById(java.util.UUID.fromString(transactionId))
        .orElseThrow(() -> new TransactionNotFoundException(transactionId));

    if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
        throw new RuntimeException("Transaction cannot be voided. Current status: "
            + transaction.getStatus());
    }

    transaction.setStatus(TransactionStatus.VOIDED);
    transaction = transactionRepository.save(transaction);
    log.info("Transaction {} VOIDED", transaction.getId());
    return mapToResponse(transaction, "Payment voided successfully");
}

public java.util.List<PaymentResponse> getMerchantTransactions(String merchantId) {
    return transactionRepository.findByMerchantId(merchantId)
        .stream()
        .map(t -> mapToResponse(t, "OK"))
        .collect(java.util.stream.Collectors.toList());
}
}




// This service class contains the core business logic for processing payments. It handles idempotency, card data extraction, transaction state management, and simulates a fraud check. The getTransaction method allows retrieval of transaction details by ID.
// Note: In a real implementation, the fraud check would be more complex and likely involve integration with an external fraud detection service. Additionally, the authorization step would involve communication with a payment processor or bank. This implementation is simplified for demonstration purposes and will be enhanced in later phases.
// Remember to handle sensitive information carefully in logs and responses, especially in a payment gateway context. Avoid logging sensitive data such as full card numbers or personal information.
// To use this service, simply inject it into your controller and call the initiatePayment method with a PaymentRequest object. The service will handle the rest of the processing and return a PaymentResponse object with the transaction details and status.
// Example usage in a controller:
// @RestController
// @RequestMapping("/api/payments")
// public class PaymentController {
//     private final PaymentService paymentService;
//     public PaymentController(PaymentService paymentService) {
//         this.paymentService = paymentService;
//     }
//     @PostMapping
//     public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
//         PaymentResponse response = paymentService.initiatePayment(request);
//         return ResponseEntity.ok(response);
//     }
//     @GetMapping("/{id}")
//     public ResponseEntity<PaymentResponse> getPayment(@PathVariable String id) {
//         PaymentResponse response = paymentService.getTransaction(id);
//         return ResponseEntity.ok(response);
//     }
// }
// End of PaymentService.java