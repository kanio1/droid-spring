package com.droid.bss.infrastructure.graphql.persisted;

import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates GraphQL queries before persistence
 */
@Slf4j
public class PersistedQueryValidator {

    private static final Pattern DISALLOWED_PATTERNS = Pattern.compile(
        "(?i)(__schema|__typename|IntrospectionQuery)"
    );

    private static final int MAX_QUERY_DEPTH = 10;
    private static final int MAX_QUERY_SIZE = 65536; // 64KB
    private static final int MAX_OPERATION_NAME_LENGTH = 50;
    private static final Set<String> ALLOWED_OPERATION_TYPES = Set.of("query", "mutation", "subscription");

    private final Validator graphqlValidator;

    public PersistedQueryValidator() {
        this.graphqlValidator = new Validator();
    }

    /**
     * Validate a GraphQL query
     */
    public ValidationResult validate(String query, String operationName) {
        try {
            // Check query size
            if (query.getBytes().length > MAX_QUERY_SIZE) {
                return ValidationResult.failure("Query exceeds maximum size of " + MAX_QUERY_SIZE + " bytes");
            }

            // Check for disallowed patterns
            if (DISALLOWED_PATTERNS.matcher(query).find()) {
                return ValidationResult.failure("Query contains disallowed patterns");
            }

            // Parse the query
            Parser parser = new Parser();
            Document document = parser.parseDocument(query);

            // Check for operation definition
            if (document.getDefinitions().isEmpty()) {
                return ValidationResult.failure("Query must contain at least one operation definition");
            }

            // Validate each operation
            for (OperationDefinition operation : document.getDefinitionsOfType(OperationDefinition.class)) {
                ValidationResult operationValidation = validateOperation(operation, operationName);
                if (!operationValidation.isValid()) {
                    return operationValidation;
                }
            }

            // Run GraphQL validation
            List<ValidationError> errors = graphqlValidator.validate(document);
            if (!errors.isEmpty()) {
                String errorMessage = errors.stream()
                    .map(ValidationError::getMessage)
                    .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                    .orElse("Validation failed");
                return ValidationResult.failure("GraphQL validation failed: " + errorMessage);
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.warn("Query validation error", e);
            return ValidationResult.failure("Query parsing/validation error: " + e.getMessage());
        }
    }

    /**
     * Validate a specific operation
     */
    private ValidationResult validateOperation(OperationDefinition operation, String expectedOperationName) {
        // Check operation type
        String operationType = operation.getOperation().toString().toLowerCase();
        if (!ALLOWED_OPERATION_TYPES.contains(operationType)) {
            return ValidationResult.failure("Operation type '" + operationType + "' is not allowed");
        }

        // Check operation name
        if (operation.getName() != null) {
            String name = operation.getName();
            if (name.length() > MAX_OPERATION_NAME_LENGTH) {
                return ValidationResult.failure("Operation name exceeds maximum length of " + MAX_OPERATION_NAME_LENGTH);
            }

            if (expectedOperationName != null && !name.equals(expectedOperationName)) {
                return ValidationResult.failure("Operation name mismatch: expected '" + expectedOperationName + "', found '" + name + "'");
            }
        }

        // Check query depth
        int depth = calculateDepth(operation);
        if (depth > MAX_QUERY_DEPTH) {
            return ValidationResult.failure("Query depth exceeds maximum of " + MAX_QUERY_DEPTH);
        }

        return ValidationResult.success();
    }

    /**
     * Calculate the maximum depth of a query
     */
    private int calculateDepth(OperationDefinition operation) {
        // Simple depth calculation
        // In a real implementation, you would traverse the selection set
        String queryString = operation.toString();
        int maxDepth = 1;
        int currentDepth = 0;

        for (char c : queryString.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}') {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
