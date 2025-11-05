package com.droid.bss.domain.asset;

import com.droid.bss.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SIM card entity for mobile/cellular asset tracking
 */
@Entity
@Table(name = "sim_cards")
@Where(clause = "deleted_at IS NULL")
public class SIMCardEntity extends BaseEntity {

    @NotNull
    @Column(name = "iccid", unique = true, length = 20)
    private String iccid; // Integrated Circuit Card Identifier

    @Column(name = "msisdn", length = 15)
    private String msisdn; // Mobile Station International Subscriber Directory Number

    @Column(name = "imsi", length = 15, unique = true)
    private String imsi; // International Mobile Subscriber Identity

    @Column(name = "pin", length = 8)
    private String pin;

    @Column(name = "puk", length = 8)
    private String puk;

    @Column(name = "ki", length = 32)
    private String ki; // Authentication key

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SIMCardStatus status;

    @Column(name = "network_operator", length = 255)
    private String networkOperator;

    @Column(name = "apn", length = 255)
    private String apn; // Access Point Name

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "deactivation_date")
    private LocalDate deactivationDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "assigned_to_type", length = 100)
    private String assignedToType; // CUSTOMER, DEVICE

    @Column(name = "assigned_to_id", length = 36)
    private String assignedToId;

    @Column(name = "assigned_to_name", length = 255)
    private String assignedToName;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "data_limit_mb")
    private Long dataLimitMb;

    @Column(name = "data_used_mb")
    private Long dataUsedMb = 0L;

    @Column(name = "voice_limit_minutes")
    private Long voiceLimitMinutes;

    @Column(name = "voice_used_minutes")
    private Long voiceUsedMinutes = 0L;

    @Column(name = "sms_limit")
    private Long smsLimit;

    @Column(name = "sms_used")
    private Long smsUsed = 0L;

    @Column(name = "last_usage_date")
    private LocalDate lastUsageDate;

    @Column(name = "notes", length = 2000)
    private String notes;

    public SIMCardEntity() {
    }

    public SIMCardEntity(String iccid, SIMCardStatus status) {
        this.iccid = iccid;
        this.status = status;
    }

    // Helper methods
    public boolean isAvailable() {
        return status == SIMCardStatus.AVAILABLE;
    }

    public boolean isAssigned() {
        return status == SIMCardStatus.ASSIGNED;
    }

    public boolean hasDataAvailable() {
        if (dataLimitMb == null) return true;
        return dataUsedMb < dataLimitMb;
    }

    public boolean hasVoiceAvailable() {
        if (voiceLimitMinutes == null) return true;
        return voiceUsedMinutes < voiceLimitMinutes;
    }

    public boolean hasSmsAvailable() {
        if (smsLimit == null) return true;
        return smsUsed < smsLimit;
    }

    public boolean isExpired() {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(LocalDate.now());
    }

    public long getRemainingDataMb() {
        if (dataLimitMb == null) return Long.MAX_VALUE;
        return dataLimitMb - (dataUsedMb != null ? dataUsedMb : 0L);
    }

    public long getRemainingVoiceMinutes() {
        if (voiceLimitMinutes == null) return Long.MAX_VALUE;
        return voiceLimitMinutes - (voiceUsedMinutes != null ? voiceUsedMinutes : 0L);
    }

    public long getRemainingSms() {
        if (smsLimit == null) return Long.MAX_VALUE;
        return smsLimit - (smsUsed != null ? smsUsed : 0L);
    }

    public void assignTo(String type, String id, String name) {
        this.assignedToType = type;
        this.assignedToId = id;
        this.assignedToName = name;
        this.assignedDate = LocalDate.now();
        this.status = SIMCardStatus.ASSIGNED;
    }

    public void release() {
        this.assignedToType = null;
        this.assignedToId = null;
        this.assignedToName = null;
        this.assignedDate = null;
        this.status = SIMCardStatus.AVAILABLE;
    }

    public void addDataUsage(long mb) {
        this.dataUsedMb = (this.dataUsedMb != null ? this.dataUsedMb : 0L) + mb;
        this.lastUsageDate = LocalDate.now();
    }

    public void addVoiceUsage(long minutes) {
        this.voiceUsedMinutes = (this.voiceUsedMinutes != null ? this.voiceUsedMinutes : 0L) + minutes;
        this.lastUsageDate = LocalDate.now();
    }

    public void addSmsUsage(long count) {
        this.smsUsed = (this.smsUsed != null ? this.smsUsed : 0L) + count;
        this.lastUsageDate = LocalDate.now();
    }

    // Getters and Setters
    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getPuk() {
        return puk;
    }

    public void setPuk(String puk) {
        this.puk = puk;
    }

    public String getKi() {
        return ki;
    }

    public void setKi(String ki) {
        this.ki = ki;
    }

    public SIMCardStatus getStatus() {
        return status;
    }

    public void setStatus(SIMCardStatus status) {
        this.status = status;
    }

    public String getNetworkOperator() {
        return networkOperator;
    }

    public void setNetworkOperator(String networkOperator) {
        this.networkOperator = networkOperator;
    }

    public String getApn() {
        return apn;
    }

    public void setApn(String apn) {
        this.apn = apn;
    }

    public LocalDate getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDate activationDate) {
        this.activationDate = activationDate;
    }

    public LocalDate getDeactivationDate() {
        return deactivationDate;
    }

    public void setDeactivationDate(LocalDate deactivationDate) {
        this.deactivationDate = deactivationDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getAssignedToType() {
        return assignedToType;
    }

    public void setAssignedToType(String assignedToType) {
        this.assignedToType = assignedToType;
    }

    public String getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(String assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public Long getDataLimitMb() {
        return dataLimitMb;
    }

    public void setDataLimitMb(Long dataLimitMb) {
        this.dataLimitMb = dataLimitMb;
    }

    public Long getDataUsedMb() {
        return dataUsedMb;
    }

    public void setDataUsedMb(Long dataUsedMb) {
        this.dataUsedMb = dataUsedMb;
    }

    public Long getVoiceLimitMinutes() {
        return voiceLimitMinutes;
    }

    public void setVoiceLimitMinutes(Long voiceLimitMinutes) {
        this.voiceLimitMinutes = voiceLimitMinutes;
    }

    public Long getVoiceUsedMinutes() {
        return voiceUsedMinutes;
    }

    public void setVoiceUsedMinutes(Long voiceUsedMinutes) {
        this.voiceUsedMinutes = voiceUsedMinutes;
    }

    public Long getSmsLimit() {
        return smsLimit;
    }

    public void setSmsLimit(Long smsLimit) {
        this.smsLimit = smsLimit;
    }

    public Long getSmsUsed() {
        return smsUsed;
    }

    public void setSmsUsed(Long smsUsed) {
        this.smsUsed = smsUsed;
    }

    public LocalDate getLastUsageDate() {
        return lastUsageDate;
    }

    public void setLastUsageDate(LocalDate lastUsageDate) {
        this.lastUsageDate = lastUsageDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
