package com.droid.bss.domain.partner;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partner commission rules
 */
@Entity
@Table(name = "partner_commission_rules")
public class PartnerCommissionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private PartnerEntity partner;

    @NotNull
    @Column(name = "rule_name", length = 255)
    private String ruleName;

    @Column(name = "product_category", length = 100)
    private String productCategory; // INTERNET, VOICE, MOBILE, etc.

    @Column(name = "service_type", length = 100)
    private String serviceType; // BASIC, PREMIUM, ENTERPRISE

    @NotNull
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "minimum_sale_amount", precision = 12, scale = 2)
    private BigDecimal minimumSaleAmount;

    @Column(name = "maximum_sale_amount", precision = 12, scale = 2)
    private BigDecimal maximumSaleAmount;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_tiered", nullable = false)
    private Boolean isTiered = false;

    @Column(name = "tier_1_threshold", precision = 12, scale = 2)
    private BigDecimal tier1Threshold;

    @Column(name = "tier_1_rate", precision = 5, scale = 2)
    private BigDecimal tier1Rate;

    @Column(name = "tier_2_threshold", precision = 12, scale = 2)
    private BigDecimal tier2Threshold;

    @Column(name = "tier_2_rate", precision = 5, scale = 2)
    private BigDecimal tier2Rate;

    @Column(name = "tier_3_threshold", precision = 12, scale = 2)
    private BigDecimal tier3Threshold;

    @Column(name = "tier_3_rate", precision = 5, scale = 2)
    private BigDecimal tier3Rate;

    @Column(name = "bonus_commission_rate", precision = 5, scale = 2)
    private BigDecimal bonusCommissionRate;

    @Column(name = "bonus_threshold", precision = 12, scale = 2)
    private BigDecimal bonusThreshold;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public PartnerCommissionRule() {}

    public PartnerCommissionRule(PartnerEntity partner, String ruleName, BigDecimal commissionRate) {
        this.partner = partner;
        this.ruleName = ruleName;
        this.commissionRate = commissionRate;
        this.effectiveFrom = LocalDate.now();
    }

    public boolean isActive() {
        if (!active) return false;
        if (effectiveFrom != null && effectiveFrom.isAfter(LocalDate.now())) return false;
        if (effectiveTo != null && effectiveTo.isBefore(LocalDate.now())) return false;
        return true;
    }

    public boolean appliesTo(BigDecimal saleAmount) {
        if (!isActive()) return false;
        if (minimumSaleAmount != null && saleAmount.compareTo(minimumSaleAmount) < 0) return false;
        if (maximumSaleAmount != null && saleAmount.compareTo(maximumSaleAmount) > 0) return false;
        return true;
    }

    public BigDecimal calculateCommission(BigDecimal saleAmount) {
        if (!appliesTo(saleAmount)) {
            return BigDecimal.ZERO;
        }

        if (Boolean.TRUE.equals(isTiered)) {
            return calculateTieredCommission(saleAmount);
        }

        return saleAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateTieredCommission(BigDecimal saleAmount) {
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal remainingAmount = saleAmount;

        // Tier 1
        if (tier1Threshold != null && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tier1Amount = remainingAmount.min(tier1Threshold);
            commission = commission.add(tier1Amount.multiply(tier1Rate).divide(BigDecimal.valueOf(100)));
            remainingAmount = remainingAmount.subtract(tier1Amount);
        }

        // Tier 2
        if (tier2Threshold != null && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tier2Amount = remainingAmount.min(tier2Threshold);
            commission = commission.add(tier2Amount.multiply(tier2Rate).divide(BigDecimal.valueOf(100)));
            remainingAmount = remainingAmount.subtract(tier2Amount);
        }

        // Tier 3
        if (tier3Threshold != null && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tier3Amount = remainingAmount.min(tier3Threshold);
            commission = commission.add(tier3Amount.multiply(tier3Rate).divide(BigDecimal.valueOf(100)));
            remainingAmount = remainingAmount.subtract(tier3Amount);
        }

        // Above tiers
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && tier3Rate != null) {
            commission = commission.add(remainingAmount.multiply(tier3Rate).divide(BigDecimal.valueOf(100)));
        }

        // Bonus commission
        if (bonusThreshold != null && bonusCommissionRate != null &&
            saleAmount.compareTo(bonusThreshold) > 0) {
            BigDecimal bonus = saleAmount.multiply(bonusCommissionRate).divide(BigDecimal.valueOf(100));
            commission = commission.add(bonus);
        }

        return commission;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PartnerEntity getPartner() {
        return partner;
    }

    public void setPartner(PartnerEntity partner) {
        this.partner = partner;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getMinimumSaleAmount() {
        return minimumSaleAmount;
    }

    public void setMinimumSaleAmount(BigDecimal minimumSaleAmount) {
        this.minimumSaleAmount = minimumSaleAmount;
    }

    public BigDecimal getMaximumSaleAmount() {
        return maximumSaleAmount;
    }

    public void setMaximumSaleAmount(BigDecimal maximumSaleAmount) {
        this.maximumSaleAmount = maximumSaleAmount;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Boolean getIsTiered() {
        return isTiered;
    }

    public void setIsTiered(Boolean isTiered) {
        this.isTiered = isTiered;
    }

    public BigDecimal getTier1Threshold() {
        return tier1Threshold;
    }

    public void setTier1Threshold(BigDecimal tier1Threshold) {
        this.tier1Threshold = tier1Threshold;
    }

    public BigDecimal getTier1Rate() {
        return tier1Rate;
    }

    public void setTier1Rate(BigDecimal tier1Rate) {
        this.tier1Rate = tier1Rate;
    }

    public BigDecimal getTier2Threshold() {
        return tier2Threshold;
    }

    public void setTier2Threshold(BigDecimal tier2Threshold) {
        this.tier2Threshold = tier2Threshold;
    }

    public BigDecimal getTier2Rate() {
        return tier2Rate;
    }

    public void setTier2Rate(BigDecimal tier2Rate) {
        this.tier2Rate = tier2Rate;
    }

    public BigDecimal getTier3Threshold() {
        return tier3Threshold;
    }

    public void setTier3Threshold(BigDecimal tier3Threshold) {
        this.tier3Threshold = tier3Threshold;
    }

    public BigDecimal getTier3Rate() {
        return tier3Rate;
    }

    public void setTier3Rate(BigDecimal tier3Rate) {
        this.tier3Rate = tier3Rate;
    }

    public BigDecimal getBonusCommissionRate() {
        return bonusCommissionRate;
    }

    public void setBonusCommissionRate(BigDecimal bonusCommissionRate) {
        this.bonusCommissionRate = bonusCommissionRate;
    }

    public BigDecimal getBonusThreshold() {
        return bonusThreshold;
    }

    public void setBonusThreshold(BigDecimal bonusThreshold) {
        this.bonusThreshold = bonusThreshold;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
