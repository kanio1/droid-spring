package com.droid.bss.application.query.product;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.product.ProductResponse;
import com.droid.bss.domain.product.ProductStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Query service for product read operations
 */
@Service
public interface ProductQueryService {

    Optional<ProductResponse> findById(String productId);

    PageResponse<ProductResponse> findAll(int page, int size, String sort);

    PageResponse<ProductResponse> findByStatus(String status, int page, int size, String sort);

    PageResponse<ProductResponse> search(String searchTerm, int page, int size, String sort);

    PageResponse<ProductResponse> findActive(int page, int size, String sort);
}
