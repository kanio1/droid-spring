package com.droid.bss.infrastructure.write.product;

import com.droid.bss.application.command.product.ProductCommandRepository;
import com.droid.bss.application.dto.product.ChangeProductStatusCommand;
import com.droid.bss.application.dto.product.CreateProductCommand;
import com.droid.bss.application.dto.product.UpdateProductCommand;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the ProductCommandRepository port
 * This is the infrastructure layer implementation
 */
@Component
public class ProductCommandRepositoryAdapter implements ProductCommandRepository {

    private final ProductRepository productRepository;

    public ProductCommandRepositoryAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public UUID create(CreateProductCommand command) {
        ProductEntity product = new ProductEntity(
            command.productCode(),
            command.name(),
            command.description(),
            command.productType(),
            command.category(),
            command.price(),
            command.currency() != null ? command.currency() : "PLN",
            command.billingPeriod(),
            command.status() != null ? command.status() : com.droid.bss.domain.product.ProductStatus.ACTIVE,
            command.validityStart(),
            command.validityEnd()
        );

        ProductEntity savedProduct = productRepository.save(product);
        return savedProduct.getId();
    }

    @Override
    public ProductEntity update(UpdateProductCommand command) {
        Optional<ProductEntity> existingProductOpt = productRepository.findById(command.id());

        if (existingProductOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + command.id());
        }

        ProductEntity existingProduct = existingProductOpt.get();

        // Check version for optimistic locking
        if (!existingProduct.getVersion().equals(command.version())) {
            throw new IllegalArgumentException("Version conflict detected");
        }

        // Update fields
        existingProduct.setProductCode(command.productCode());
        existingProduct.setName(command.name());
        existingProduct.setDescription(command.description());
        existingProduct.setProductType(command.productType());
        existingProduct.setCategory(command.category());
        existingProduct.setPrice(command.price());
        existingProduct.setCurrency(command.currency());
        existingProduct.setBillingPeriod(command.billingPeriod());
        existingProduct.setStatus(command.status());
        existingProduct.setValidityStart(command.validityStart());
        existingProduct.setValidityEnd(command.validityEnd());

        return productRepository.save(existingProduct);
    }

    @Override
    public ProductEntity changeStatus(ChangeProductStatusCommand command) {
        Optional<ProductEntity> existingProductOpt = productRepository.findById(command.id());

        if (existingProductOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + command.id());
        }

        ProductEntity existingProduct = existingProductOpt.get();

        // Check version for optimistic locking
        if (!existingProduct.getVersion().equals(command.version())) {
            throw new IllegalArgumentException("Version conflict detected");
        }

        // Update status
        existingProduct.setStatus(command.status());

        return productRepository.save(existingProduct);
    }

    @Override
    public Optional<ProductEntity> findById(UUID id) {
        return productRepository.findById(id);
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        return productRepository.existsByProductCode(productCode);
    }

    @Override
    public boolean deleteById(UUID productId) {
        Optional<ProductEntity> existingProductOpt = productRepository.findById(productId);

        if (existingProductOpt.isEmpty()) {
            return false;
        }

        ProductEntity existingProduct = existingProductOpt.get();

        // Soft delete by setting deleted_at date
        existingProduct.setDeletedAt(LocalDate.now());
        productRepository.save(existingProduct);

        return true;
    }
}
