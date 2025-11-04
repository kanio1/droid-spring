package com.droid.bss.domain.product;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity representing services, tariffs, bundles, and add-ons
 */
@Entity
@Table(name = "products")
@SQLRestriction("deleted_at IS NULL")
public class ProductEntity extends BaseEntity {

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "category", length = 20)
    private ProductCategory category;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "billing_period", nullable = false, length = 20)
    private String billingPeriod;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "validity_start")
    private LocalDate validityStart;

    @Column(name = "validity_end")
    private LocalDate validityEnd;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductFeatureEntity> features = new ArrayList<>();

    // Constructors
    public ProductEntity() {}

    public ProductEntity(
            String productCode,
            String name,
            String description,
            ProductType productType,
            ProductCategory category,
            BigDecimal price,
            String currency,
            String billingPeriod,
            ProductStatus status,
            LocalDate validityStart,
            LocalDate validityEnd
    ) {
        this.productCode = productCode;
        this.name = name;
        this.description = description;
        this.productType = productType;
        this.category = category;
        this.price = price;
        this.currency = currency;
        this.billingPeriod = billingPeriod;
        this.status = status;
        this.validityStart = validityStart;
        this.validityEnd = validityEnd;
    }

    // Business methods
    public boolean isActive() {
        return status == ProductStatus.ACTIVE &&
               (validityStart == null || !LocalDate.now().isBefore(validityStart)) &&
               (validityEnd == null || !LocalDate.now().isAfter(validityEnd));
    }

    public boolean hasFeature(String featureKey) {
        return features.stream()
            .anyMatch(feature -> feature.getFeatureKey().equals(featureKey));
    }

    public void addFeature(ProductFeatureEntity feature) {
        features.add(feature);
        feature.setProduct(this);
    }

    public void removeFeature(ProductFeatureEntity feature) {
        features.remove(feature);
        feature.setProduct(null);
    }

    // Getters and setters
    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public LocalDate getValidityStart() {
        return validityStart;
    }

    public void setValidityStart(LocalDate validityStart) {
        this.validityStart = validityStart;
    }

    public LocalDate getValidityEnd() {
        return validityEnd;
    }

    public void setValidityEnd(LocalDate validityEnd) {
        this.validityEnd = validityEnd;
    }

    public LocalDate getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDate deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<ProductFeatureEntity> getFeatures() {
        return features;
    }

    public void setFeatures(List<ProductFeatureEntity> features) {
        this.features = features;
    }
}
