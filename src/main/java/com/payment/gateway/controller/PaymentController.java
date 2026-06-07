package com.payment.gateway.controller;

import com.payment.gateway.dto.PaymentRequest;
import com.payment.gateway.dto.PaymentResponse;
import com.payment.gateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Gateway", description = "Payment processing, capture, and refund operations")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Initiate a payment",
               description = "Processes a new payment request through fraud check and authorization")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment authorized successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request from merchant: {}", request.getMerchantId());
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get transaction by ID",
               description = "Retrieves full details of a transaction")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getTransaction(
            @Parameter(description = "Transaction UUID")
            @PathVariable String transactionId) {
        log.info("Fetching transaction: {}", transactionId);
        PaymentResponse response = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Capture an authorized payment",
               description = "Captures an AUTHORIZED transaction — actually collects the money")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment captured"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "400", description = "Transaction cannot be captured")
    })
    @PostMapping("/{transactionId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @Parameter(description = "Transaction UUID")
            @PathVariable String transactionId) {
        log.info("Capture request for transaction: {}", transactionId);
        PaymentResponse response = paymentService.capturePayment(transactionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refund a captured payment",
               description = "Refunds a CAPTURED transaction — returns money to the customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment refunded"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "400", description = "Transaction cannot be refunded")
    })
    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "Transaction UUID")
            @PathVariable String transactionId) {
        log.info("Refund request for transaction: {}", transactionId);
        PaymentResponse response = paymentService.refundPayment(transactionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Void an authorized payment",
               description = "Voids an AUTHORIZED transaction before capture — cancels the payment")
    @PostMapping("/{transactionId}/void")
    public ResponseEntity<PaymentResponse> voidPayment(
            @Parameter(description = "Transaction UUID")
            @PathVariable String transactionId) {
        log.info("Void request for transaction: {}", transactionId);
        PaymentResponse response = paymentService.voidPayment(transactionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all transactions for a merchant")
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<?> getMerchantTransactions(
            @Parameter(description = "Merchant ID")
            @PathVariable String merchantId) {
        log.info("Fetching transactions for merchant: {}", merchantId);
        return ResponseEntity.ok(paymentService.getMerchantTransactions(merchantId));
    }
}