package com.droid.bss.api.product;

import com.droid.bss.application.command.product.ChangeProductStatusUseCase;
import com.droid.bss.application.command.product.CreateProductUseCase;
import com.droid.bss.application.command.product.DeleteProductUseCase;
import com.droid.bss.application.command.product.UpdateProductUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.product.ChangeProductStatusCommand;
import com.droid.bss.application.dto.product.CreateProductCommand;
import com.droid.bss.application.dto.product.ProductResponse;
import com.droid.bss.application.dto.product.UpdateProductCommand;
import com.droid.bss.application.query.product.ProductQueryService;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST Controller for Product CRUD operations
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Product management API")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final ChangeProductStatusUseCase changeProductStatusUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ProductQueryService productQueryService;

    public ProductController(
            CreateProductUseCase createProductUseCase,
            UpdateProductUseCase updateProductUseCase,
            ChangeProductStatusUseCase changeProductStatusUseCase,
            DeleteProductUseCase deleteProductUseCase,
            ProductQueryService productQueryService) {
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.changeProductStatusUseCase = changeProductStatusUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.productQueryService = productQueryService;
    }

    // Create
    @PostMapping
    @Operation(
        summary = "Create a new product",
        description = "Creates a new product with the provided details"
    )
    @ApiResponse(responseCode = "201", description = "Product created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @Audited(action = AuditAction.PRODUCT_CREATE, entityType = "Product", description = "Creating new product")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductCommand command,
            @AuthenticationPrincipal Jwt principal
    ) {
        var productId = createProductUseCase.handle(command);
        var product = productQueryService.findById(productId.toString())
                .orElseThrow(() -> new RuntimeException("Product not found after creation"));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(productId.toString())
                .toUri();

        return ResponseEntity.created(location).body(product);
    }

    // Read single
    @GetMapping("/{id}")
    @Operation(
        summary = "Get product by ID",
        description = "Retrieves a single product by its unique identifier"
    )
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable UUID id
    ) {
        return productQueryService.findById(id.toString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Read all with pagination and sorting
    @GetMapping
    @Operation(
        summary = "Get all products",
        description = "Retrieves a paginated list of all products"
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = productQueryService.findAll(page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }

    // Read by status
    @GetMapping("/by-status/{status}")
    @Operation(
        summary = "Get products by status",
        description = "Retrieves products filtered by their status"
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status value")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<ProductResponse>> getProductsByStatus(
            @Parameter(description = "Product status (ACTIVE, INACTIVE, DEPRECATED)", required = true) @PathVariable String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = productQueryService.findByStatus(status, page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }

    // Search products
    @GetMapping("/search")
    @Operation(
        summary = "Search products",
        description = "Searches products by name or description"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @Parameter(description = "Search term", required = true) @RequestParam String searchTerm,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = productQueryService.search(searchTerm, page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }

    // Find active products
    @GetMapping("/active")
    @Operation(
        summary = "Get active products",
        description = "Retrieves only active products (status=ACTIVE and within validity period)"
    )
    @ApiResponse(responseCode = "200", description = "Active products retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<ProductResponse>> getActiveProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = productQueryService.findActive(page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }

    // Update
    @PutMapping("/{id}")
    @Operation(
        summary = "Update product",
        description = "Updates an existing product with new details"
    )
    @ApiResponse(responseCode = "200", description = "Product updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @Audited(action = AuditAction.PRODUCT_UPDATE, entityType = "Product", description = "Updating product {id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateProductCommand command
    ) {
        if (!id.equals(command.id())) {
            return ResponseEntity.badRequest().build();
        }

        var updatedProduct = updateProductUseCase.handle(command);
        var response = ProductResponse.from(updatedProduct);

        return ResponseEntity.ok(response);
    }

    // Change status
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Change product status",
        description = "Changes the status of an existing product"
    )
    @ApiResponse(responseCode = "200", description = "Product status changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @Audited(action = AuditAction.PRODUCT_UPDATE, entityType = "Product", description = "Changing status for product {id}")
    public ResponseEntity<ProductResponse> changeProductStatus(
            @Parameter(description = "Product ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody ChangeProductStatusCommand command
    ) {
        if (!id.equals(command.id())) {
            return ResponseEntity.badRequest().build();
        }

        var updatedProduct = changeProductStatusUseCase.handle(command);
        var response = ProductResponse.from(updatedProduct);

        return ResponseEntity.ok(response);
    }

    // Delete (soft delete via setting deleted_at)
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete product",
        description = "Soft deletes a product by setting the deleted_at date"
    )
    @ApiResponse(responseCode = "204", description = "Product deleted successfully")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @Audited(action = AuditAction.PRODUCT_DELETE, entityType = "Product", description = "Deleting product {id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Product version for optimistic locking", required = true) @RequestParam @NotNull Long version
    ) {
        boolean deleted = deleteProductUseCase.handle(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
