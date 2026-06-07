package com.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FraudCheckResult {

    public enum Decision {
        ALLOW,   // Score 0-29  — process normally
        REVIEW,  // Score 30-59 — flag for human review but allow
        BLOCK    // Score 60+   — decline immediately
    }

    private Decision decision;
    private int riskScore;
    private List<String> triggeredRules;
    private String summary;

    public boolean isBlocked() {
        return decision == Decision.BLOCK;
    }

    public boolean needsReview() {
        return decision == Decision.REVIEW;
    }
}