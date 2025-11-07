package com.droid.bss.domain.security;

import lombok.Builder;
import lombok.Data;

/**
 * Compliance check result
 */
@Data
@Builder
public class ComplianceCheck {

    private String checkType;
    private Long totalChecks;
    private Long passed;
    private Long failed;
    private Double compliancePercent;

    public boolean isCompliant() {
        return compliancePercent != null && compliancePercent >= 95.0;
    }

    public String getComplianceStatus() {
        if (isCompliant()) return "COMPLIANT";
        if (compliancePercent != null && compliancePercent >= 80.0) return "PARTIAL";
        return "NON_COMPLIANT";
    }
}
