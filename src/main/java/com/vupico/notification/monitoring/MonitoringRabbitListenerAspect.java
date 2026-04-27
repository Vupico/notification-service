package com.vupico.notification.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AOP for public methods on classes in {@code com.vupico.notification.consumer} annotated with
 * {@link MonitoringComponent} (e.g. {@code @RabbitListener} entry points) — no HTTP context; uses a minimal
 * {@code source=rabbitmq} request context.
 */
@Aspect
@Component
@Order
public class MonitoringRabbitListenerAspect {

    private final RequestMonitoringLogger monitoringLogger;
    private final MonitoringClientProperties monitoringClientProperties;

    public MonitoringRabbitListenerAspect(RequestMonitoringLogger monitoringLogger,
                                            MonitoringClientProperties monitoringClientProperties) {
        this.monitoringLogger = monitoringLogger;
        this.monitoringClientProperties = monitoringClientProperties;
    }

    @Pointcut("@within(com.vupico.notification.monitoring.MonitoringComponent)"
            + " && execution(public * com.vupico.notification.consumer..*(..))")
    public void monitoredConsumerMethod() {
    }

    @Around("monitoredConsumerMethod()")
    public Object aroundConsumer(ProceedingJoinPoint pjp) throws Throwable {
        Class<?> clazz = AopUtils.getTargetClass(pjp.getTarget());
        MonitoringComponent mc = clazz.getAnnotation(MonitoringComponent.class);
        if (mc == null) {
            return pjp.proceed();
        }
        String domainKey = mc.value();
        String domain = monitoringClientProperties.domain(domainKey, defaultDomainLabel(domainKey));
        String component = clazz.getSimpleName();
        String method = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        String op = "RABBIT_" + component + "_" + method;
        Map<String, Object> reqCtx = Map.of("source", "rabbitmq", "category", "consumer");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", method);
        addArgHints(pjp.getArgs(), details);
        String message = "Rabbit " + component + "." + method;

        try {
            Object out = pjp.proceed();
            details.put("result", "SUCCESS");
            monitoringLogger.info(clazz, message,
                    MonitoringUtils.logData(op, domain, component, "SUCCESS", reqCtx, details));
            return out;
        } catch (Throwable ex) {
            details.put("errorMessage", MonitoringUtils.safeErrorMessage(ex));
            details.put("result", "FAILED");
            monitoringLogger.error(clazz, message + " failed",
                    MonitoringUtils.logData(op, domain, component, "FAILED", reqCtx, details), ex);
            throw ex;
        }
    }

    private static void addArgHints(Object[] args, Map<String, Object> details) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a instanceof List<?> list) {
                details.put("arg" + i + "BatchSize", list.size());
            } else if (a != null && a.getClass().getName().endsWith("Channel")) {
                details.put("arg" + i + "Type", "Channel");
            } else if (a instanceof String s) {
                details.put("arg" + i + "StringLen", s.length());
            }
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
}
