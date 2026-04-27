package com.vupico.notification.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AOP for REST handlers on classes annotated with {@link MonitoringComponent} in {@code com.vupico.notification.web}.
 * Aligns with vupico-ai {@code MonitoringControllerAspect} / dap-duo-core log payload shape.
 */
@Aspect
@Component
@Order
public class MonitoringControllerAspect {

    private final RequestMonitoringLogger monitoringLogger;
    private final MonitoringClientProperties monitoringClientProperties;
    private final ObjectMapper objectMapper;

    public MonitoringControllerAspect(RequestMonitoringLogger monitoringLogger,
                                      MonitoringClientProperties monitoringClientProperties,
                                      ObjectMapper objectMapper) {
        this.monitoringLogger = monitoringLogger;
        this.monitoringClientProperties = monitoringClientProperties;
        this.objectMapper = objectMapper;
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)"
            + " && @within(com.vupico.notification.monitoring.MonitoringComponent)"
            + " && execution(public * com.vupico.notification.web..*(..))")
    public void monitoredControllerEndpoint() {
    }

    @Around("monitoredControllerEndpoint()")
    public Object aroundController(ProceedingJoinPoint pjp) throws Throwable {
        Class<?> controllerClass = AopUtils.getTargetClass(pjp.getTarget());
        MonitoringComponent mc = controllerClass.getAnnotation(MonitoringComponent.class);
        if (mc == null) {
            return pjp.proceed();
        }

        HttpServletRequest request = currentRequest();
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String methodName = method.getName();
        String component = controllerClass.getSimpleName();
        String domainKey = mc.value();
        String domain = monitoringClientProperties.domain(domainKey, defaultDomainLabel(domainKey));
        String op = buildOperationCode(request, component, methodName);
        Map<String, Object> reqCtx = request != null
                ? MonitoringUtils.requestContext(request)
                : Map.of();

        Map<String, Object> payloadSnapshot = buildPayloadSnapshot(pjp, request);
        Map<String, Object> baseDetails = new LinkedHashMap<>();
        String requestBodyJson = buildRequestBodyJsonString(payloadSnapshot);
        if (requestBodyJson != null) {
            baseDetails.put("requestBodyJson", requestBodyJson);
        }

        String humanMessage = humanMessage(request, methodName);

        try {
            Object result = pjp.proceed();

            if (result instanceof ResponseEntity<?> re) {
                var status = re.getStatusCode();
                if (status.is4xxClientError() || status.is5xxServerError()) {
                    Map<String, Object> details = new LinkedHashMap<>(baseDetails);
                    details.put("httpStatus", status.value());
                    details.put("result", "http_error");
                    if (status.is5xxServerError()) {
                        monitoringLogger.error(controllerClass, humanMessage,
                                MonitoringUtils.logData(op, domain, component, "FAILED", reqCtx, details),
                                null);
                    } else {
                        monitoringLogger.info(controllerClass, humanMessage,
                                MonitoringUtils.logData(op, domain, component, "FAILED", reqCtx, details));
                    }
                    return result;
                }
            }

            Map<String, Object> details = new LinkedHashMap<>(baseDetails);
            details.put("result", "success");
            if (result instanceof ResponseEntity<?> ok) {
                details.put("httpStatus", ok.getStatusCode().value());
            }
            monitoringLogger.info(controllerClass, humanMessage,
                    MonitoringUtils.logData(op, domain, component, "SUCCESS", reqCtx, details));
            return result;
        } catch (Throwable ex) {
            Map<String, Object> details = new LinkedHashMap<>(baseDetails);
            details.put("errorMessage", MonitoringUtils.safeErrorMessage(ex));
            monitoringLogger.error(controllerClass, humanMessage + " failed",
                    MonitoringUtils.logData(op, domain, component, "FAILED", reqCtx, details), ex);
            throw ex;
        }
    }

    private static String defaultDomainLabel(String domainKey) {
        if (domainKey == null || domainKey.isBlank()) {
            return "Notification";
        }
        String[] parts = domainKey.split("-");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }

    private static String buildOperationCode(HttpServletRequest request, String controllerSimple, String methodName) {
        String http = request != null ? request.getMethod().toUpperCase() : "UNKNOWN";
        return http + "_" + controllerSimple + "_" + methodName;
    }

    private static String humanMessage(HttpServletRequest request, String methodName) {
        if (request == null) {
            return methodName;
        }
        return request.getMethod() + " " + request.getRequestURI();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest();
    }

    private String buildRequestBodyJsonString(Map<String, Object> payloadSnapshot) {
        if (payloadSnapshot == null || payloadSnapshot.isEmpty()) {
            return null;
        }
        if (payloadSnapshot.size() == 1) {
            Object only = payloadSnapshot.values().iterator().next();
            return MonitoringUtils.toSafeRequestBodyJson(only);
        }
        return MonitoringUtils.toSafeRequestBodyJson(payloadSnapshot);
    }

    private Map<String, Object> buildPayloadSnapshot(ProceedingJoinPoint pjp, HttpServletRequest request) {
        if (request == null || !isMutatingHttpMethod(request.getMethod())) {
            return Map.of();
        }
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (shouldSkipArgument(arg)) {
                continue;
            }
            String name = (names != null && i < names.length && names[i] != null) ? names[i] : "arg" + i;
            out.put(name, toLoggableArgument(name, arg));
        }
        return out;
    }

    private static boolean isMutatingHttpMethod(String method) {
        if (method == null) {
            return false;
        }
        return switch (method.toUpperCase()) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    private static boolean shouldSkipArgument(Object arg) {
        if (arg == null) {
            return true;
        }
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof Model
                || arg instanceof Errors
                || arg instanceof SessionStatus;
    }

    private Object toLoggableArgument(String paramName, Object arg) {
        if (arg instanceof MultipartFile f) {
            return MonitoringPayloadSanitizer.multipartSummary(objectMapper, f);
        }
        if (arg instanceof MultipartFile[] files) {
            List<Object> list = new ArrayList<>(files.length);
            for (MultipartFile f : files) {
                list.add(MonitoringPayloadSanitizer.multipartSummary(objectMapper, f));
            }
            return list;
        }
        if (arg instanceof String s) {
            return s.length() > 4000
                    ? MonitoringPayloadSanitizer.stringPayloadSummary(objectMapper, s, 800)
                    : arg;
        }
        if (arg instanceof Principal) {
            return ((Principal) arg).getName();
        }
        return arg;
    }
}
