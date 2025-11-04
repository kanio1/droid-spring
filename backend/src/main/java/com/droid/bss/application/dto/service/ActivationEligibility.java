package com.droid.bss.application.dto.service;

/**
 * Result of activation eligibility check
 */
public record ActivationEligibility(
        boolean eligible,
        String reason
) {
}
