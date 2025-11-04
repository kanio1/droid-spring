package com.droid.bss.domain.product;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;

/**
 * Product feature entity for configurable parameters
 */
@Entity
@Table(name = "product_features")
public class ProductFeatureEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "feature_value", nullable = false, columnDefinition = "TEXT")
    private String featureValue;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "data_type", nullable = false, length = 20)
    private FeatureDataType dataType;

    @Column(name = "is_configurable")
    private Boolean isConfigurable = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // Constructors
    public ProductFeatureEntity() {}

    public ProductFeatureEntity(
            ProductEntity product,
            String featureKey,
            String featureValue,
            FeatureDataType dataType,
            Boolean isConfigurable,
            Integer displayOrder
    ) {
        this.product = product;
        this.featureKey = featureKey;
        this.featureValue = featureValue;
        this.dataType = dataType;
        this.isConfigurable = isConfigurable;
        this.displayOrder = displayOrder;
    }

    // Business methods
    public boolean isNumeric() {
        return dataType == FeatureDataType.NUMBER;
    }

    public boolean isBoolean() {
        return dataType == FeatureDataType.BOOLEAN;
    }

    public boolean isString() {
        return dataType == FeatureDataType.STRING;
    }

    public boolean isJson() {
        return dataType == FeatureDataType.JSON;
    }

    // Getters and setters
    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public String getFeatureValue() {
        return featureValue;
    }

    public void setFeatureValue(String featureValue) {
        this.featureValue = featureValue;
    }

    public FeatureDataType getDataType() {
        return dataType;
    }

    public void setDataType(FeatureDataType dataType) {
        this.dataType = dataType;
    }

    public Boolean getIsConfigurable() {
        return isConfigurable;
    }

    public void setIsConfigurable(Boolean isConfigurable) {
        this.isConfigurable = isConfigurable;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
