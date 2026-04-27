package com.vupico.notification.monitoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupico.monitoring.util.MonitoringPayloadUtils;
import com.vupico.monitoring.util.MonitoringRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Helpers for VUPI monitoring payloads (aligned with dap-duo-core / vupico-ai).
 */
public final class MonitoringUtils {

    private static final ObjectMapper REQUEST_BODY_JSON_MAPPER = new ObjectMapper();

    public static final String VUPI_LOG_TYPE = "VUPI";

    private MonitoringUtils() {
    }

    public static String toRequestBodyJson(Object body) {
        if (body == null) {
            return null;
        }
        try {
            return REQUEST_BODY_JSON_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            try {
                return REQUEST_BODY_JSON_MAPPER.writeValueAsString(
                        Map.of("serializationError", MonitoringPayloadUtils.safeErrorMessage(e)));
            } catch (JsonProcessingException ignored) {
                return "{\"serializationError\":\"failed\"}";
            }
        }
    }

    public static String toSafeRequestBodyJson(Object body) {
        if (body == null) {
            return null;
        }
        try {
            JsonNode tree = MonitoringPayloadSanitizer.sanitizeTree(REQUEST_BODY_JSON_MAPPER, body);
            return REQUEST_BODY_JSON_MAPPER.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            return toRequestBodyJson(Map.of("safeSerializationError", MonitoringPayloadUtils.safeErrorMessage(e)));
        }
    }

    public static String getUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .map(principal -> {
                    String fromJwt = jwtClaimString(principal, "sub");
                    if (fromJwt != null) {
                        return fromJwt;
                    }
                    fromJwt = jwtClaimString(principal, "preferred_username");
                    if (fromJwt != null) {
                        return fromJwt;
                    }
                    fromJwt = jwtClaimString(principal, "email");
                    if (fromJwt != null) {
                        return fromJwt;
                    }
                    if (isOAuth2JwtPrincipal(principal)) {
                        return "anonymous";
                    }
                    return principal.toString();
                })
                .orElse("system");
    }

    public static String getUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(auth -> {
                    Object principal = auth.getPrincipal();
                    String u = jwtClaimString(principal, "preferred_username");
                    if (u != null) {
                        return u;
                    }
                    u = jwtClaimString(principal, "email");
                    if (u != null) {
                        return u;
                    }
                    if (isOAuth2JwtPrincipal(principal)) {
                        return auth.getName();
                    }
                    return auth.getName();
                })
                .orElse("system");
    }

    private static boolean isOAuth2JwtPrincipal(Object principal) {
        return principal != null
                && "org.springframework.security.oauth2.jwt.Jwt".equals(principal.getClass().getName());
    }

    private static String jwtClaimString(Object principal, String claim) {
        if (principal == null || !isOAuth2JwtPrincipal(principal)) {
            return null;
        }
        try {
            Method m = principal.getClass().getMethod("getClaimAsString", String.class);
            Object v = m.invoke(principal, claim);
            return v != null ? String.valueOf(v) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static Map<String, Object> requestContext(HttpServletRequest request) {
        return MonitoringRequestUtils.requestContext(request);
    }

    @SafeVarargs
    public static Map<String, Object> mergeMaps(Map<String, Object>... maps) {
        return MonitoringPayloadUtils.mergeMaps(maps);
    }

    public static Map<String, Object> baseMetadata(String operation, String domain, String component) {
        return MonitoringPayloadUtils.baseMetadata(
                operation,
                domain,
                component,
                MonitoringClientPropertiesHolder.integrationOrDefault("notification-service"));
    }

    public static String safeErrorMessage(Throwable e) {
        return MonitoringPayloadUtils.safeErrorMessage(e);
    }

    public static String getClientIp(HttpServletRequest request) {
        return MonitoringRequestUtils.getClientIp(request);
    }

    public static String getSessionId(HttpServletRequest request) {
        return MonitoringRequestUtils.getSessionId(request);
    }

    public static String getProcessId() {
        return MonitoringRequestUtils.getProcessId();
    }

    public static String getThreadId() {
        return MonitoringRequestUtils.getThreadId();
    }

    public static String normalizeServiceName(String serviceName) {
        return MonitoringRequestUtils.normalizeServiceName(serviceName);
    }

    public static Map<String, Object> logData(
            String operation,
            String domain,
            String component,
            String status,
            Map<String, Object> requestContext,
            Map<String, Object> details) {
        return MonitoringPayloadUtils.logData(
                operation,
                domain,
                component,
                status,
                MonitoringClientPropertiesHolder.integrationOrDefault("notification-service"),
                requestContext,
                details);
    }

    public static Map<String, Object> logData(
            String operation,
            String domain,
            String component,
            String status,
            String integration,
            Map<String, Object> requestContext,
            Map<String, Object> details) {
        return MonitoringPayloadUtils.logData(operation, domain, component, status, integration, requestContext, details);
    }
}
