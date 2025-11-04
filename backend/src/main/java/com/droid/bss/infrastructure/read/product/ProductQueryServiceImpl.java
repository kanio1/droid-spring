package com.droid.bss.infrastructure.read.product;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.product.ProductResponse;
import com.droid.bss.application.query.product.ProductQueryService;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductStatus;
import com.droid.bss.domain.product.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ProductQueryService for product read operations
 */
@Component
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Cacheable(value = "products", key = "'id:' + #productId")
    public Optional<ProductResponse> findById(String productId) {
        UUID id = UUID.fromString(productId);
        return productRepository.findById(id)
                .map(ProductResponse::from);
    }

    @Override
    @Cacheable(value = "products", key = "'all:' + #page + ':' + #size + ':' + #sort")
    public PageResponse<ProductResponse> findAll(int page, int size, String sort) {
        String[] sortFields = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortFields));

        Page<ProductEntity> productPage = productRepository.findAll(pageRequest);

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();

        return PageResponse.of(
            responses,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements()
        );
    }

    @Override
    @Cacheable(value = "products", key = "'status:' + #status + ':' + #page + ':' + #size + ':' + #sort")
    public PageResponse<ProductResponse> findByStatus(String status, int page, int size, String sort) {
        ProductStatus productStatus;
        try {
            productStatus = ProductStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        String[] sortFields = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortFields));

        Page<ProductEntity> productPage = productRepository.findByStatus(productStatus, pageRequest);

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();

        return PageResponse.of(
            responses,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements()
        );
    }

    @Override
    public PageResponse<ProductResponse> search(String searchTerm, int page, int size, String sort) {
        String[] sortFields = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortFields));

        Page<ProductEntity> productPage = productRepository.searchProducts(searchTerm, pageRequest);

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();

        return PageResponse.of(
            responses,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements()
        );
    }

    @Override
    @Cacheable(value = "products", key = "'active:' + #page + ':' + #size + ':' + #sort")
    public PageResponse<ProductResponse> findActive(int page, int size, String sort) {
        String[] sortFields = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortFields));

        Page<ProductEntity> productPage = productRepository.findActiveProducts(ProductStatus.ACTIVE, pageRequest);

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();

        return PageResponse.of(
            responses,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements()
        );
    }

    private String[] parseSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return new String[]{"createdAt"};
        }

        return sort.split(",");
    }
}
