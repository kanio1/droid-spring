/**
 * Embedded entity for tenant-specific settings
 *
 * Contains configuration options specific to a tenant
 */
package com.droid.bss.domain.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.HashMap;
import java.util.Map;

@Embeddable
public class TenantSettings {

    @Column(name = "allow_self_registration")
    private Boolean allowSelfRegistration = true;

    @Column(name = "require_email_verification")
    private Boolean requireEmailVerification = true;

    @Column(name = "two_factor_required")
    private Boolean twoFactorRequired = false;

    @Column(name = "session_timeout_minutes")
    @Min(15)
    @Max(1440) // Max 24 hours
    private Integer sessionTimeoutMinutes = 60;

    @Column(name = "password_policy_min_length")
    @Min(8)
    @Max(128)
    private Integer passwordPolicyMinLength = 8;

    @Column(name = "password_policy_require_uppercase")
    private Boolean passwordPolicyRequireUppercase = true;

    @Column(name = "password_policy_require_lowercase")
    private Boolean passwordPolicyRequireLowercase = true;

    @Column(name = "password_policy_require_numbers")
    private Boolean passwordPolicyRequireNumbers = true;

    @Column(name = "password_policy_require_symbols")
    private Boolean passwordPolicyRequireSymbols = true;

    @Column(name = "allow_api_access")
    private Boolean allowApiAccess = true;

    @Column(name = "api_key_required")
    private Boolean apiKeyRequired = false;

    @Column(name = "webhook_enabled")
    private Boolean webhookEnabled = true;

    @Column(name = "data_retention_days")
    @Min(30)
    @Max(3650) // Max 10 years
    private Integer dataRetentionDays = 365;

    @Column(name = "enable_audit_log")
    private Boolean enableAuditLog = true;

    @Column(name = "allow_file_uploads")
    private Boolean allowFileUploads = true;

    @Column(name = "max_file_size_mb")
    @Min(1)
    @Max(100)
    private Integer maxFileSizeMb = 10;

    @Column(name = "allowed_file_types")
    private String allowedFileTypes; // CSV: "pdf,doc,docx,txt,jpg,png"

    @Column(name = "enable_realtime_notifications")
    private Boolean enableRealtimeNotifications = true;

    @Column(name = "enable_sso")
    private Boolean enableSso = false;

    @Column(name = "sso_provider")
    private String ssoProvider; // SAML, OAuth2, OIDC

    @Column(name = "sso_config", columnDefinition = "TEXT")
    private String ssoConfig; // JSON configuration

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "suspension_reason")
    private String suspensionReason;

    @Column(name = "billing_cycle")
    private String billingCycle; // monthly, quarterly, annually

    @Column(name = "trial_days")
    @Min(0)
    @Max(90)
    private Integer trialDays = 14;

    @Column(name = "grace_period_days")
    @Min(0)
    @Max(30)
    private Integer gracePeriodDays = 7;

    @Column(name = "overage_rate_per_unit")
    private Double overageRatePerUnit;

    @Column(name = "custom_fields_config", columnDefinition = "TEXT")
    private String customFieldsConfig; // JSON for custom fields

    @Column(name = "feature_flags", columnDefinition = "TEXT")
    private String featureFlags; // JSON for feature toggles

    @Column(name = "additional_settings", columnDefinition = "TEXT")
    private String additionalSettings; // JSON for any additional settings

    // Constructors
    public TenantSettings() {}

    // Business methods
    public boolean isFeatureEnabled(String feature) {
        if (featureFlags == null) return false;
        try {
            // Parse JSON and check if feature is enabled
            // Simplified for now
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void setFeatureEnabled(String feature, boolean enabled) {
        if (featureFlags == null) {
            featureFlags = "{}";
        }
        // Parse, set, and write back
        // Simplified for now
        featureFlags = featureFlags.replace("}", "\"" + feature + "\":" + enabled + "}");
    }

    public boolean isFileTypeAllowed(String fileType) {
        if (allowedFileTypes == null) return true;
        return allowedFileTypes.contains(fileType.toLowerCase());
    }

    public Map<String, Object> getCustomFields() {
        if (customFieldsConfig == null) {
            return new HashMap<>();
        }
        // Parse JSON and return map
        // Simplified for now
        return new HashMap<>();
    }

    // Getters and Setters
    public Boolean getAllowSelfRegistration() {
        return allowSelfRegistration;
    }

    public void setAllowSelfRegistration(Boolean allowSelfRegistration) {
        this.allowSelfRegistration = allowSelfRegistration;
    }

    public Boolean getRequireEmailVerification() {
        return requireEmailVerification;
    }

    public void setRequireEmailVerification(Boolean requireEmailVerification) {
        this.requireEmailVerification = requireEmailVerification;
    }

    public Boolean getTwoFactorRequired() {
        return twoFactorRequired;
    }

    public void setTwoFactorRequired(Boolean twoFactorRequired) {
        this.twoFactorRequired = twoFactorRequired;
    }

    public Integer getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public Integer getPasswordPolicyMinLength() {
        return passwordPolicyMinLength;
    }

    public void setPasswordPolicyMinLength(Integer passwordPolicyMinLength) {
        this.passwordPolicyMinLength = passwordPolicyMinLength;
    }

    public Boolean getPasswordPolicyRequireUppercase() {
        return passwordPolicyRequireUppercase;
    }

    public void setPasswordPolicyRequireUppercase(Boolean passwordPolicyRequireUppercase) {
        this.passwordPolicyRequireUppercase = passwordPolicyRequireUppercase;
    }

    public Boolean getPasswordPolicyRequireLowercase() {
        return passwordPolicyRequireLowercase;
    }

    public void setPasswordPolicyRequireLowercase(Boolean passwordPolicyRequireLowercase) {
        this.passwordPolicyRequireLowercase = passwordPolicyRequireLowercase;
    }

    public Boolean getPasswordPolicyRequireNumbers() {
        return passwordPolicyRequireNumbers;
    }

    public void setPasswordPolicyRequireNumbers(Boolean passwordPolicyRequireNumbers) {
        this.passwordPolicyRequireNumbers = passwordPolicyRequireNumbers;
    }

    public Boolean getPasswordPolicyRequireSymbols() {
        return passwordPolicyRequireSymbols;
    }

    public void setPasswordPolicyRequireSymbols(Boolean passwordPolicyRequireSymbols) {
        this.passwordPolicyRequireSymbols = passwordPolicyRequireSymbols;
    }

    public Boolean getAllowApiAccess() {
        return allowApiAccess;
    }

    public void setAllowApiAccess(Boolean allowApiAccess) {
        this.allowApiAccess = allowApiAccess;
    }

    public Boolean getApiKeyRequired() {
        return apiKeyRequired;
    }

    public void setApiKeyRequired(Boolean apiKeyRequired) {
        this.apiKeyRequired = apiKeyRequired;
    }

    public Boolean getWebhookEnabled() {
        return webhookEnabled;
    }

    public void setWebhookEnabled(Boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }

    public Integer getDataRetentionDays() {
        return dataRetentionDays;
    }

    public void setDataRetentionDays(Integer dataRetentionDays) {
        this.dataRetentionDays = dataRetentionDays;
    }

    public Boolean getEnableAuditLog() {
        return enableAuditLog;
    }

    public void setEnableAuditLog(Boolean enableAuditLog) {
        this.enableAuditLog = enableAuditLog;
    }

    public Boolean getAllowFileUploads() {
        return allowFileUploads;
    }

    public void setAllowFileUploads(Boolean allowFileUploads) {
        this.allowFileUploads = allowFileUploads;
    }

    public Integer getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(Integer maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public String getAllowedFileTypes() {
        return allowedFileTypes;
    }

    public void setAllowedFileTypes(String allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
    }

    public Boolean getEnableRealtimeNotifications() {
        return enableRealtimeNotifications;
    }

    public void setEnableRealtimeNotifications(Boolean enableRealtimeNotifications) {
        this.enableRealtimeNotifications = enableRealtimeNotifications;
    }

    public Boolean getEnableSso() {
        return enableSso;
    }

    public void setEnableSso(Boolean enableSso) {
        this.enableSso = enableSso;
    }

    public String getSsoProvider() {
        return ssoProvider;
    }

    public void setSsoProvider(String ssoProvider) {
        this.ssoProvider = ssoProvider;
    }

    public String getSsoConfig() {
        return ssoConfig;
    }

    public void setSsoConfig(String ssoConfig) {
        this.ssoConfig = ssoConfig;
    }

    public String getCustomCss() {
        return customCss;
    }

    public void setCustomCss(String customCss) {
        this.customCss = customCss;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void setSuspensionReason(String suspensionReason) {
        this.suspensionReason = suspensionReason;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(Integer trialDays) {
        this.trialDays = trialDays;
    }

    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public Double getOverageRatePerUnit() {
        return overageRatePerUnit;
    }

    public void setOverageRatePerUnit(Double overageRatePerUnit) {
        this.overageRatePerUnit = overageRatePerUnit;
    }

    public String getCustomFieldsConfig() {
        return customFieldsConfig;
    }

    public void setCustomFieldsConfig(String customFieldsConfig) {
        this.customFieldsConfig = customFieldsConfig;
    }

    public String getFeatureFlags() {
        return featureFlags;
    }

    public void setFeatureFlags(String featureFlags) {
        this.featureFlags = featureFlags;
    }

    public String getAdditionalSettings() {
        return additionalSettings;
    }

    public void setAdditionalSettings(String additionalSettings) {
        this.additionalSettings = additionalSettings;
    }
}
