package com.droid.bss.domain.service;

/**
 * Result of activation eligibility check
 */
public record ActivationEligibility(
        boolean eligible,
        String reason
) {
}
