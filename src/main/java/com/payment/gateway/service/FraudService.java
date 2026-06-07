package com.payment.gateway.service;

import com.payment.gateway.dto.FraudCheckResult;
import com.payment.gateway.dto.PaymentRequest;
import com.payment.gateway.entity.Transaction;
import com.payment.gateway.enums.TransactionStatus;
import com.payment.gateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudService {

    private final TransactionRepository transactionRepository;

    // Thresholds — in a real system these come from a config database
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("200000");
    private static final int VELOCITY_WINDOW_MINUTES = 1;
    private static final int VELOCITY_MAX_TRANSACTIONS = 5;
    private static final int VELOCITY_BLOCK_TRANSACTIONS = 10;

    public FraudCheckResult evaluate(PaymentRequest request) {

        List<String> triggeredRules = new ArrayList<>();
        int totalScore = 0;

        log.debug("Starting fraud evaluation for merchant: {} amount: {} {}",
            request.getMerchantId(), request.getAmount(), request.getCurrency());

        // ── Rule 1: Velocity Check ──────────────────────────────────────
        // How many transactions has this merchant made in the last minute?
        int recentCount = countRecentTransactions(request.getMerchantId());

        if (recentCount >= VELOCITY_BLOCK_TRANSACTIONS) {
            totalScore += 60;
            triggeredRules.add(String.format(
                "VELOCITY_BLOCK: %d transactions in last %d minute(s) — limit is %d",
                recentCount, VELOCITY_WINDOW_MINUTES, VELOCITY_BLOCK_TRANSACTIONS));
            log.warn("Velocity block triggered for merchant {}: {} recent transactions",
                request.getMerchantId(), recentCount);

        } else if (recentCount >= VELOCITY_MAX_TRANSACTIONS) {
            totalScore += 40;
            triggeredRules.add(String.format(
                "VELOCITY_WARNING: %d transactions in last %d minute(s)",
                recentCount, VELOCITY_WINDOW_MINUTES));
            log.warn("Velocity warning for merchant {}: {} recent transactions",
                request.getMerchantId(), recentCount);
        }

        // ── Rule 2: Amount Check ────────────────────────────────────────
        // Large amounts are riskier
        if (request.getAmount().compareTo(VERY_HIGH_AMOUNT_THRESHOLD) >= 0) {
            totalScore += 35;
            triggeredRules.add(String.format(
                "VERY_HIGH_AMOUNT: %.2f %s exceeds threshold of %.2f",
                request.getAmount(), request.getCurrency(), VERY_HIGH_AMOUNT_THRESHOLD));
            log.warn("Very high amount detected: {} {}", request.getAmount(), request.getCurrency());

        } else if (request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            totalScore += 20;
            triggeredRules.add(String.format(
                "HIGH_AMOUNT: %.2f %s exceeds threshold of %.2f",
                request.getAmount(), request.getCurrency(), HIGH_AMOUNT_THRESHOLD));
            log.info("High amount flagged: {} {}", request.getAmount(), request.getCurrency());
        }

        // ── Rule 3: New Merchant Check ──────────────────────────────────
        // Merchants with very few total transactions get extra scrutiny
        int totalMerchantTransactions = countTotalMerchantTransactions(request.getMerchantId());

        if (totalMerchantTransactions == 0) {
            totalScore += 15;
            triggeredRules.add("NEW_MERCHANT: First transaction for this merchant ID");
            log.info("New merchant detected: {}", request.getMerchantId());

        } else if (totalMerchantTransactions < 5) {
            totalScore += 10;
            triggeredRules.add(String.format(
                "LOW_VOLUME_MERCHANT: Only %d previous transactions",
                totalMerchantTransactions));
        }

        // ── Rule 4: Card BIN Check ──────────────────────────────────────
        // Cards starting with certain patterns are prepaid/virtual cards
        // which are higher risk (often used in fraud)
        String cardBin = request.getCardNumber().substring(0, 6);
        if (isPrepaidBin(cardBin)) {
            totalScore += 20;
            triggeredRules.add("PREPAID_CARD: Card BIN indicates prepaid/virtual card");
            log.info("Prepaid card BIN detected: {}xxxxxxxxxxxx", cardBin);
        }

        // ── Rule 5: Round Number Check ──────────────────────────────────
        // Fraudsters often test cards with round numbers like 1000, 5000
        if (isRoundNumber(request.getAmount()) &&
            request.getAmount().compareTo(new BigDecimal("1000")) >= 0) {
            totalScore += 10;
            triggeredRules.add("ROUND_NUMBER: Suspiciously round amount — common in card testing");
        }

        // ── Make Decision ───────────────────────────────────────────────
        FraudCheckResult.Decision decision;
        if (totalScore >= 60) {
            decision = FraudCheckResult.Decision.BLOCK;
        } else if (totalScore >= 30) {
            decision = FraudCheckResult.Decision.REVIEW;
        } else {
            decision = FraudCheckResult.Decision.ALLOW;
        }

        String summary = String.format("Risk score: %d — Decision: %s — Rules triggered: %d",
            totalScore, decision, triggeredRules.size());

        log.info("Fraud evaluation complete for merchant {}: {}",
            request.getMerchantId(), summary);

        return FraudCheckResult.builder()
            .decision(decision)
            .riskScore(totalScore)
            .triggeredRules(triggeredRules)
            .summary(summary)
            .build();
    }

    // Count transactions for this merchant in the last N minutes
    private int countRecentTransactions(String merchantId) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        List<Transaction> recent = transactionRepository.findByMerchantId(merchantId);

        return (int) recent.stream()
            .filter(t -> t.getCreatedAt() != null &&
                         t.getCreatedAt().isAfter(windowStart) &&
                         t.getStatus() != TransactionStatus.DECLINED)
            .count();
    }

    // Count all non-declined transactions for this merchant
    private int countTotalMerchantTransactions(String merchantId) {
        List<Transaction> all = transactionRepository.findByMerchantId(merchantId);
        return (int) all.stream()
            .filter(t -> t.getStatus() != TransactionStatus.DECLINED)
            .count();
    }

    // Known prepaid/virtual card BIN prefixes (simplified for demo)
    private boolean isPrepaidBin(String bin) {
        List<String> prepaidBins = List.of(
            "400115", "400516", "414720", "416997"
        );
        return prepaidBins.contains(bin);
    }

    // Check if amount is a suspiciously round number
    private boolean isRoundNumber(BigDecimal amount) {
        return amount.remainder(new BigDecimal("1000"))
                     .compareTo(BigDecimal.ZERO) == 0;
    }
}