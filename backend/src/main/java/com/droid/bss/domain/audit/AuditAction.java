package com.droid.bss.domain.audit;

/**
 * Audit Action Types
 *
 * Enumerates all possible actions that can be audited
 */
public enum AuditAction {

    // Authentication Actions
    LOGIN("User login"),
    LOGOUT("User logout"),
    LOGIN_FAILED("Failed login attempt"),

    // Customer Management
    CUSTOMER_CREATE("Customer created"),
    CUSTOMER_UPDATE("Customer updated"),
    CUSTOMER_DELETE("Customer deleted"),
    CUSTOMER_VIEW("Customer viewed"),

    // Address Management
    ADDRESS_CREATE("Address created"),
    ADDRESS_UPDATE("Address updated"),
    ADDRESS_DELETE("Address deleted"),
    ADDRESS_SET_PRIMARY("Primary address set"),

    // Product Management
    PRODUCT_CREATE("Product created"),
    PRODUCT_UPDATE("Product updated"),
    PRODUCT_DELETE("Product deleted"),
    PRODUCT_ACTIVATE("Product activated"),
    PRODUCT_DEACTIVATE("Product deactivated"),

    // Order Management
    ORDER_CREATE("Order created"),
    ORDER_UPDATE("Order updated"),
    ORDER_DELETE("Order deleted"),
    ORDER_CANCEL("Order canceled"),
    ORDER_COMPLETE("Order completed"),
    ORDER_PROCESS("Order processing started"),

    // Subscription Management
    SUBSCRIPTION_CREATE("Subscription created"),
    SUBSCRIPTION_UPDATE("Subscription updated"),
    SUBSCRIPTION_DELETE("Subscription deleted"),
    SUBSCRIPTION_CANCEL("Subscription canceled"),
    SUBSCRIPTION_ACTIVATE("Subscription activated"),
    SUBSCRIPTION_RENEW("Subscription renewed"),

    // Payment Management
    PAYMENT_CREATE("Payment created"),
    PAYMENT_UPDATE("Payment updated"),
    PAYMENT_DELETE("Payment deleted"),
    PAYMENT_PROCESS("Payment processed"),
    PAYMENT_REFUND("Payment refunded"),
    PAYMENT_REVERSE("Payment reversed"),
    PAYMENT_FAIL("Payment failed"),

    // Invoice Management
    INVOICE_CREATE("Invoice created"),
    INVOICE_UPDATE("Invoice updated"),
    INVOICE_SEND("Invoice sent"),
    INVOICE_PAID("Invoice paid"),
    INVOICE_OVERDUE("Invoice overdue"),
    INVOICE_CANCEL("Invoice canceled"),

    // Service Management
    SERVICE_CREATE("Service created"),
    SERVICE_UPDATE("Service updated"),
    SERVICE_DELETE("Service deleted"),

    // User Management
    USER_CREATE("User created"),
    USER_UPDATE("User updated"),
    USER_DELETE("User deleted"),

    // Partner Management
    PARTNER_CREATE("Partner created"),
    PARTNER_UPDATE("Partner updated"),
    PARTNER_DELETE("Partner deleted"),

    // Asset Management
    ASSET_CREATE("Asset created"),
    ASSET_UPDATE("Asset updated"),
    ASSET_DELETE("Asset deleted"),

    // Billing Management
    BILLING_CREATE("Billing record created"),
    BILLING_UPDATE("Billing record updated"),

    // Fraud Management
    FRAUD_CREATE("Fraud alert created"),
    FRAUD_UPDATE("Fraud alert updated"),
    FRAUD_DELETE("Fraud alert deleted"),

    // Workforce Management
    WORKFORCE_CREATE("Workforce record created"),
    WORKFORCE_UPDATE("Workforce record updated"),
    WORKFORCE_DELETE("Workforce record deleted"),

    // Alert Management
    ALERT_CREATE("Alert created"),
    ALERT_UPDATE("Alert updated"),
    ALERT_DELETE("Alert deleted"),

    // Metric Management
    METRIC_CREATE("Metric created"),

    // Monitoring Management
    MONITORING_CREATE("Monitoring record created"),
    MONITORING_UPDATE("Monitoring record updated"),
    MONITORING_DELETE("Monitoring record deleted"),

    // System Actions
    DATA_EXPORT("Data exported"),
    DATA_IMPORT("Data imported"),
    CONFIG_CHANGE("Configuration changed"),
    BACKUP("Database backup"),
    RESTORE("Database restore"),

    // Security Events
    PASSWORD_CHANGE("Password changed"),
    PASSWORD_RESET("Password reset"),
    PERMISSION_GRANT("Permission granted"),
    PERMISSION_REVOKE("Permission revoked"),
    ROLE_ASSIGN("Role assigned"),
    ROLE_UNASSIGN("Role unassigned"),

    // API Access
    API_ACCESS("API accessed"),
    API_KEY_CREATE("API key created"),
    API_KEY_REVOKE("API key revoked"),
    API_RATE_LIMIT("API rate limit exceeded"),

    // Generic CRUD
    CREATE("Create operation"),
    READ("Read operation"),
    UPDATE("Update operation"),
    DELETE("Delete operation");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
