package com.droid.bss.application.command.product;

import com.droid.bss.application.dto.product.ChangeProductStatusCommand;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for changing product status
 */
@Component
@Transactional
public class ChangeProductStatusUseCase {

    private final ProductCommandRepository productRepository;

    public ChangeProductStatusUseCase(ProductCommandRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductEntity handle(ChangeProductStatusCommand command) {
        Optional<ProductEntity> existingProduct = productRepository.findById(command.id());

        if (existingProduct.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + command.id());
        }

        ProductStatus newStatus = command.status();

        // Update status via repository
        ProductEntity updatedProduct = productRepository.changeStatus(command);
        return updatedProduct;
    }
}
