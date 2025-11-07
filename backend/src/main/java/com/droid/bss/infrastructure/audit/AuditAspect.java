package com.droid.bss.infrastructure.audit;

import com.droid.bss.domain.audit.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Aspect
 *
 * Automatically logs sensitive operations using AOP
 * This ensures all critical operations are audited without manual intervention
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(100) // Execute after other aspects like transactions
public class AuditAspect {

    private final AuditService auditService;

    /**
     * Automatically log operations marked with @Audited annotation
     */
    @Around("@annotation(audited)")
    public Object auditOperation(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        AuditAction action = audited.action();
        String entityType = audited.entityType();
        String descriptionTemplate = audited.description();

        // Extract method parameters for context
        String entityId = extractEntityId(joinPoint);
        Map<String, Object> parameters = extractParameters(joinPoint);
        String description = buildDescription(descriptionTemplate, joinPoint, parameters);

        // Create audit event
        AuditEvent event = AuditEvent.builder()
                .action(action)
                .entityType(entityType.isEmpty() ? joinPoint.getTarget().getClass().getSimpleName() : entityType)
                .entityId(entityId)
                .description(description)
                .metadata(parameters)
                .source("BSS-AOP")
                .version("1.0")
                .build();

        // Enrich with security context
        enrichWithSecurityContext(event);

        // Enrich with request context
        enrichWithRequestContext(event);

        // Execute with audit logging
        return auditService.executeWithAudit(event, joinPoint::proceed);
    }

    /**
     * Extract entity ID from method parameters
     */
    private String extractEntityId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }

        // Try to find ID-like parameter
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            // Check for UUID
            if (arg instanceof UUID) {
                return arg.toString();
            }

            // Check for String ID field
            try {
                java.lang.reflect.Field idField = arg.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object idValue = idField.get(arg);
                if (idValue != null) {
                    return idValue.toString();
                }
            } catch (Exception e) {
                // Field not found, continue
            }
        }

        return null;
    }

    /**
     * Extract method parameters as map
     */
    private Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
        Map<String, Object> parameters = new HashMap<>();
        Object[] args = joinPoint.getArgs();

        if (args == null || args.length == 0) {
            return parameters;
        }

        String[] paramNames = joinPoint.getSignature().getName().split("\\.");
        for (int i = 0; i < args.length; i++) {
            String paramName = "arg" + i;
            Object arg = args[i];

            if (arg != null) {
                // Sanitize sensitive data
                Object sanitizedValue = sanitizeArgument(arg);
                parameters.put(paramName, sanitizedValue);
            }
        }

        return parameters;
    }

    /**
     * Sanitize sensitive data from arguments
     */
    private Object sanitizeArgument(Object arg) {
        if (arg == null) {
            return null;
        }

        // Handle string fields with sensitive data
        if (arg instanceof String) {
            String str = (String) arg;
            if (isSensitiveField(str)) {
                return "[REDACTED]";
            }
            return str.length() > 100 ? str.substring(0, 100) + "..." : str;
        }

        // For objects, return class name and ID if available
        StringBuilder builder = new StringBuilder(arg.getClass().getSimpleName());

        try {
            java.lang.reflect.Field idField = arg.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(arg);
            if (idValue != null) {
                builder.append("(id=").append(idValue).append(")");
            }
        } catch (Exception e) {
            // No ID field
        }

        return builder.toString();
    }

    /**
     * Check if string contains sensitive data
     */
    private boolean isSensitiveField(String value) {
        if (value == null) {
            return false;
        }

        String lowerValue = value.toLowerCase();
        return lowerValue.contains("password") ||
               lowerValue.contains("secret") ||
               lowerValue.contains("token") ||
               lowerValue.contains("credential") ||
               lowerValue.matches(".*[0-9]{4}-?[0-9]{4}-?[0-9]{4}.*"); // Credit card pattern
    }

    /**
     * Build description from template
     */
    private String buildDescription(String template, ProceedingJoinPoint joinPoint, Map<String, Object> parameters) {
        if (template == null || template.isEmpty()) {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            return className + "." + methodName;
        }

        // Simple template replacement
        String description = template;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "null";
            description = description.replace(placeholder, value);
        }

        return description;
    }

    /**
     * Enrich event with security context
     */
    private void enrichWithSecurityContext(AuditEvent event) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                event.setUserId(authentication.getName());
                event.setUsername(authentication.getName());

                // Extract authorities
                StringBuilder authorities = new StringBuilder();
                authentication.getAuthorities().forEach(authority -> {
                    if (authorities.length() > 0) {
                        authorities.append(",");
                    }
                    authorities.append(authority.getAuthority());
                });
                event.getMetadata().put("authorities", authorities.toString());
            }
        } catch (Exception e) {
            log.debug("Failed to extract security context", e);
        }
    }

    /**
     * Enrich event with HTTP request context
     */
    private void enrichWithRequestContext(AuditEvent event) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            if (request != null) {
                event.setIpAddress(getClientIpAddress(request));
                event.setUserAgent(request.getHeader("User-Agent"));
                event.setSessionId(request.getSession().getId());
                event.setRequestId(request.getHeader("X-Request-ID"));
                event.getMetadata().put("httpMethod", request.getMethod());
                event.getMetadata().put("requestURI", request.getRequestURI());
            }
        } catch (Exception e) {
            log.debug("Failed to extract request context", e);
        }
    }

    /**
     * Extract client IP address (handles proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
