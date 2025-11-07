package com.droid.bss.domain.partner;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Partner entity for managing external partners (resellers, distributors, MVNO)
 */
@Entity
@Table(name = "partners")
public class PartnerEntity extends BaseEntity {

    @NotNull
    @Column(name = "partner_code", unique = true, length = 50)
    private String partnerCode;

    @NotNull
    @Column(length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", length = 50)
    private PartnerType partnerType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PartnerStatus status = PartnerStatus.ACTIVE;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "settlement_frequency", length = 50)
    private String settlementFrequency; // MONTHLY, QUARTERLY, ANNUALLY

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms; // NET_15, NET_30, NET_60

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "current_balance", precision = 12, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_commission", precision = 15, scale = 2)
    private BigDecimal totalCommission = BigDecimal.ZERO;

    @Column(name = "territory", length = 500)
    private String territory;

    @Column(name = "market_segment", length = 100)
    private String marketSegment;

    @Column(name = "contract_value", precision = 15, scale = 2)
    private BigDecimal contractValue = BigDecimal.ZERO;

    @Column(name = "services_provided", columnDefinition = "TEXT")
    private String servicesProvided;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartnerCommissionRule> commissionRules = new ArrayList<>();

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartnerSettlement> settlements = new ArrayList<>();

    public PartnerEntity() {}

    public PartnerEntity(String partnerCode, String name, PartnerType partnerType) {
        this.partnerCode = partnerCode;
        this.name = name;
        this.partnerType = partnerType;
    }

    // Business methods
    public boolean isActive() {
        return status == PartnerStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return status == PartnerStatus.SUSPENDED;
    }

    public boolean isTerminated() {
        return status == PartnerStatus.TERMINATED;
    }

    public boolean canProcessOrders() {
        return isActive() && contractStartDate != null &&
                contractStartDate.isBefore(LocalDate.now()) &&
                (contractEndDate == null || contractEndDate.isAfter(LocalDate.now()));
    }

    public boolean hasCommissionRate() {
        return commissionRate != null && commissionRate.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal calculateCommission(BigDecimal saleAmount) {
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return saleAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100));
    }

    public void addSale(BigDecimal amount) {
        this.totalSales = this.totalSales.add(amount);
        this.totalCommission = this.totalCommission.add(calculateCommission(amount));
        this.currentBalance = this.currentBalance.add(calculateCommission(amount));
    }

    public void processSettlement(BigDecimal amount) {
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    public void suspend(String reason) {
        this.status = PartnerStatus.SUSPENDED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Suspended: " + reason + " on " + LocalDate.now();
    }

    public void activate() {
        this.status = PartnerStatus.ACTIVE;
    }

    public void terminate(String reason) {
        this.status = PartnerStatus.TERMINATED;
        this.contractEndDate = LocalDate.now();
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Terminated: " + reason + " on " + LocalDate.now();
    }

    public void addCommissionRule(PartnerCommissionRule rule) {
        commissionRules.add(rule);
        rule.setPartner(this);
    }

    public void addSettlement(PartnerSettlement settlement) {
        settlements.add(settlement);
        settlement.setPartner(this);
    }

    public boolean isContractValid() {
        if (contractStartDate == null) return false;
        if (contractStartDate.isAfter(LocalDate.now())) return false;
        if (contractEndDate != null && contractEndDate.isBefore(LocalDate.now())) return false;
        return true;
    }

    public int getRemainingContractDays() {
        if (contractEndDate == null) return Integer.MAX_VALUE;
        long daysDiff = contractEndDate.toEpochDay() - LocalDate.now().toEpochDay();
        return (int) Math.max(daysDiff, Integer.MIN_VALUE);
    }

    // Getters and Setters
    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PartnerType getPartnerType() {
        return partnerType;
    }

    public void setPartnerType(PartnerType partnerType) {
        this.partnerType = partnerType;
    }

    public PartnerStatus getStatus() {
        return status;
    }

    public void setStatus(PartnerStatus status) {
        this.status = status;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public LocalDate getContractStartDate() {
        return contractStartDate;
    }

    public void setContractStartDate(LocalDate contractStartDate) {
        this.contractStartDate = contractStartDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDate contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public String getSettlementFrequency() {
        return settlementFrequency;
    }

    public void setSettlementFrequency(String settlementFrequency) {
        this.settlementFrequency = settlementFrequency;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getTotalCommission() {
        return totalCommission;
    }

    public void setTotalCommission(BigDecimal totalCommission) {
        this.totalCommission = totalCommission;
    }

    public String getTerritory() {
        return territory;
    }

    public void setTerritory(String territory) {
        this.territory = territory;
    }

    public String getMarketSegment() {
        return marketSegment;
    }

    public void setMarketSegment(String marketSegment) {
        this.marketSegment = marketSegment;
    }

    public BigDecimal getContractValue() {
        return contractValue;
    }

    public void setContractValue(BigDecimal contractValue) {
        this.contractValue = contractValue;
    }

    public String getServicesProvided() {
        return servicesProvided;
    }

    public void setServicesProvided(String servicesProvided) {
        this.servicesProvided = servicesProvided;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PartnerCommissionRule> getCommissionRules() {
        return commissionRules;
    }

    public void setCommissionRules(List<PartnerCommissionRule> commissionRules) {
        this.commissionRules = commissionRules;
    }

    public List<PartnerSettlement> getSettlements() {
        return settlements;
    }

    public void setSettlements(List<PartnerSettlement> settlements) {
        this.settlements = settlements;
    }
}
