package com.droid.bss.api.graphql;

import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductStatus;
import com.droid.bss.infrastructure.read.ProductReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL Controller for Product-related queries and mutations
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProductGraphQLController {

    private final ProductReadRepository productRepository;

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ProductEntity> product(@Argument UUID id) {
        log.debug("Fetching product with id: {}", id);
        return CompletableFuture.supplyAsync(() ->
            productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id))
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<ProductEntity>> products(
            @Argument Optional<Integer> page,
            @Argument Optional<Integer> size,
            @Argument Optional<ProductStatus> status,
            @Argument Optional<String> search) {

        log.debug("Fetching products with filters");
        return CompletableFuture.supplyAsync(() -> {
            List<ProductEntity> products = productRepository.findAll();

            if (status.isPresent()) {
                products = products.stream()
                    .filter(p -> p.getStatus() == status.get())
                    .collect(java.util.stream.Collectors.toList());
            }

            if (search.isPresent() && !search.get().trim().isEmpty()) {
                String query = search.get().toLowerCase();
                products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query) ||
                                (p.getDescription() != null && p.getDescription().toLowerCase().contains(query)) ||
                                p.getSku().toLowerCase().contains(query))
                    .collect(java.util.stream.Collectors.toList());
            }

            return products;
        });
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<ProductEntity>> searchProducts(@Argument String query) {
        log.debug("Searching products with query: {}", query);
        return CompletableFuture.supplyAsync(() -> {
            List<ProductEntity> products = productRepository.findAll();
            String lowerQuery = query.toLowerCase();

            return products.stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerQuery)) ||
                            p.getSku().toLowerCase().contains(lowerQuery) ||
                            (p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery)) ||
                            (p.getTags() != null && p.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))))
                .collect(java.util.stream.Collectors.toList());
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ProductEntity> createProduct(@Argument("input") CreateProductInput input) {
        log.info("Creating product with SKU: {}", input.getSku());
        return CompletableFuture.supplyAsync(() -> {
            ProductEntity product = ProductEntity.builder()
                .id(UUID.randomUUID())
                .name(input.getName())
                .description(input.getDescription())
                .sku(input.getSku())
                .price(input.getPrice())
                .cost(input.getCost())
                .currency(input.getCurrency() != null ? input.getCurrency() : "PLN")
                .status(input.getStatus() != null ? input.getStatus() : ProductStatus.DRAFT)
                .category(input.getCategory())
                .tags(input.getTags() != null ? input.getTags() : Arrays.asList())
                .imageUrl(input.getImageUrl())
                .stockQuantity(input.getStockQuantity())
                .weight(input.getWeight())
                .dimensions(input.getDimensions())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return productRepository.save(product);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ProductEntity> updateProduct(
            @Argument UUID id,
            @Argument("input") UpdateProductInput input) {

        log.info("Updating product: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

            if (input.getName() != null) product.setName(input.getName());
            if (input.getDescription() != null) product.setDescription(input.getDescription());
            if (input.getSku() != null) product.setSku(input.getSku());
            if (input.getPrice() != null) product.setPrice(input.getPrice());
            if (input.getCost() != null) product.setCost(input.getCost());
            if (input.getCurrency() != null) product.setCurrency(input.getCurrency());
            if (input.getStatus() != null) product.setStatus(input.getStatus());
            if (input.getCategory() != null) product.setCategory(input.getCategory());
            if (input.getTags() != null) product.setTags(input.getTags());
            if (input.getImageUrl() != null) product.setImageUrl(input.getImageUrl());
            if (input.getStockQuantity() != null) product.setStockQuantity(input.getStockQuantity());
            if (input.getWeight() != null) product.setWeight(input.getWeight());
            if (input.getDimensions() != null) product.setDimensions(input.getDimensions());
            product.setUpdatedAt(LocalDateTime.now());

            return productRepository.save(product);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<Boolean> deleteProduct(@Argument UUID id) {
        log.warn("Deleting product: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

            product.setStatus(ProductStatus.DISCONTINUED);
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);

            return true;
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ProductEntity> changeProductStatus(
            @Argument UUID id,
            @Argument ProductStatus status) {

        log.info("Changing product status to: {} for product: {}", status, id);
        return CompletableFuture.supplyAsync(() -> {
            ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

            product.setStatus(status);
            product.setUpdatedAt(LocalDateTime.now());

            return productRepository.save(product);
        });
    }

    // ========== INPUT CLASSES ==========

    public static class CreateProductInput {
        private String name;
        private String description;
        private String sku;
        private BigDecimal price;
        private BigDecimal cost;
        private String currency;
        private ProductStatus status;
        private String category;
        private List<String> tags;
        private String imageUrl;
        private Integer stockQuantity;
        private BigDecimal weight;
        private String dimensions;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public ProductStatus getStatus() { return status; }
        public void setStatus(ProductStatus status) { this.status = status; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }

        public String getDimensions() { return dimensions; }
        public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    }

    public static class UpdateProductInput {
        private String name;
        private String description;
        private String sku;
        private BigDecimal price;
        private BigDecimal cost;
        private String currency;
        private ProductStatus status;
        private String category;
        private List<String> tags;
        private String imageUrl;
        private Integer stockQuantity;
        private BigDecimal weight;
        private String dimensions;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public ProductStatus getStatus() { return status; }
        public void setStatus(ProductStatus status) { this.status = status; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }

        public String getDimensions() { return dimensions; }
        public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    }
}
