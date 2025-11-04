package com.droid.bss.application.command.product;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for deleting a product (soft delete)
 */
@Component
@Transactional
public class DeleteProductUseCase {

    private final ProductCommandRepository productRepository;

    public DeleteProductUseCase(ProductCommandRepository productRepository) {
        this.productRepository = productRepository;
    }

    public boolean handle(UUID productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        return productRepository.deleteById(productId);
    }
}
