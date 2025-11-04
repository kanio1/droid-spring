package com.droid.bss.application.command.product;

import com.droid.bss.application.dto.product.ChangeProductStatusCommand;
import com.droid.bss.application.dto.product.CreateProductCommand;
import com.droid.bss.application.dto.product.UpdateProductCommand;
import com.droid.bss.domain.product.ProductEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for product command operations
 * This is the hexagonal architecture port that use cases depend on
 */
public interface ProductCommandRepository {

    UUID create(CreateProductCommand command);

    ProductEntity update(UpdateProductCommand command);

    ProductEntity changeStatus(ChangeProductStatusCommand command);

    Optional<ProductEntity> findById(UUID id);

    boolean existsByProductCode(String productCode);

    boolean deleteById(UUID productId);
}
