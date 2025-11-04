package com.droid.bss.application.command.product;

import com.droid.bss.application.dto.product.UpdateProductCommand;
import com.droid.bss.domain.product.ProductEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for updating an existing product
 */
@Component
@Transactional
public class UpdateProductUseCase {

    private final ProductCommandRepository productRepository;

    public UpdateProductUseCase(ProductCommandRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductEntity handle(UpdateProductCommand command) {
        Optional<ProductEntity> existingProduct = productRepository.findById(command.id());

        if (existingProduct.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + command.id());
        }

        // Update product via repository
        ProductEntity updatedProduct = productRepository.update(command);
        return updatedProduct;
    }
}
