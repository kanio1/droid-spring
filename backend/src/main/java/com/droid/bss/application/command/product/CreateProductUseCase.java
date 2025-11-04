package com.droid.bss.application.command.product;

import com.droid.bss.application.dto.product.CreateProductCommand;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for creating a new product
 */
@Component
@Transactional
public class CreateProductUseCase {

    private final ProductCommandRepository productRepository;

    public CreateProductUseCase(ProductCommandRepository productRepository) {
        this.productRepository = productRepository;
    }

    public UUID handle(CreateProductCommand command) {
        // Business validation
        if (productRepository.existsByProductCode(command.productCode())) {
            throw new IllegalArgumentException("Product with code %s already exists".formatted(command.productCode()));
        }

        // Create product via repository
        UUID productId = productRepository.create(command);
        return productId;
    }
}
