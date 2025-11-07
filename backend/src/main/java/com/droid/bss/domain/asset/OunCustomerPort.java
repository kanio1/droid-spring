package com.droid.bss.domain.asset;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Customer port mapping for OUN unit
 */
@Entity
@Table(name = "oun_customer_ports")
public class OunCustomerPort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oun_unit_id")
    private OunEntity ounUnit;

    @NotNull
    @Column(name = "port_number")
    private Integer portNumber;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "service_type", length = 100)
    private String serviceType; // INTERNET, VOICE, TV, COMBINED

    @Column(name = "connection_status", length = 50)
    private String connectionStatus; // ACTIVE, INACTIVE, SUSPENDED

    @Column(name = "connected_since")
    private LocalDateTime connectedSince;

    @Column(name = "connection_speed_mbps")
    private Double connectionSpeedMbps;

    @Column(name = "ont_device_id", length = 100)
    private String ontDeviceId;

    @Column(name = "ont_serial", length = 255)
    private String ontSerial;

    @Column(name = "signal_strength_dbm")
    private Double signalStrengthDbm;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    public OunCustomerPort() {
    }

    public OunCustomerPort(OunEntity ounUnit, Integer portNumber) {
        this.ounUnit = ounUnit;
        this.portNumber = portNumber;
        this.connectionStatus = "INACTIVE";
    }

    public void activate(String customerId, String customerName, String serviceType) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.serviceType = serviceType;
        this.connectionStatus = "ACTIVE";
        this.connectedSince = LocalDateTime.now();
    }

    public void suspend() {
        this.connectionStatus = "SUSPENDED";
    }

    public void deactivate() {
        this.customerId = null;
        this.customerName = null;
        this.serviceType = null;
        this.connectionStatus = "INACTIVE";
        this.connectedSince = null;
    }

    public boolean isActive() {
        return "ACTIVE".equals(connectionStatus);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OunEntity getOunUnit() {
        return ounUnit;
    }

    public void setOunUnit(OunEntity ounUnit) {
        this.ounUnit = ounUnit;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public LocalDateTime getConnectedSince() {
        return connectedSince;
    }

    public void setConnectedSince(LocalDateTime connectedSince) {
        this.connectedSince = connectedSince;
    }

    public Double getConnectionSpeedMbps() {
        return connectionSpeedMbps;
    }

    public void setConnectionSpeedMbps(Double connectionSpeedMbps) {
        this.connectionSpeedMbps = connectionSpeedMbps;
    }

    public String getOntDeviceId() {
        return ontDeviceId;
    }

    public void setOntDeviceId(String ontDeviceId) {
        this.ontDeviceId = ontDeviceId;
    }

    public String getOntSerial() {
        return ontSerial;
    }

    public void setOntSerial(String ontSerial) {
        this.ontSerial = ontSerial;
    }

    public Double getSignalStrengthDbm() {
        return signalStrengthDbm;
    }

    public void setSignalStrengthDbm(Double signalStrengthDbm) {
        this.signalStrengthDbm = signalStrengthDbm;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
}
