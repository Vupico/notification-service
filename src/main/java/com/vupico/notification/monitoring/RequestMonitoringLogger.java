package com.vupico.notification.monitoring;

import com.vupico.monitoring.dto.MonitoringLogRequest;
import com.vupico.monitoring.service.MonitoringService;
import com.vupico.monitoring.util.MonitoringRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Component
public class RequestMonitoringLogger {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final MonitoringService monitoringService;
    private final ObjectProvider<HttpServletRequest> httpServletRequestProvider;
    private final MonitoringClientProperties monitoringClientProperties;

    public RequestMonitoringLogger(MonitoringService monitoringService,
                                   ObjectProvider<HttpServletRequest> httpServletRequestProvider,
                                   MonitoringClientProperties monitoringClientProperties) {
        this.monitoringService = monitoringService;
        this.httpServletRequestProvider = httpServletRequestProvider;
        this.monitoringClientProperties = monitoringClientProperties;
    }

    public void info(String message, Map<String, Object> data) {
        info("unknown", message, data);
    }

    public void info(String serviceName, String message, Map<String, Object> data) {
        monitoringService.logInfo(buildRequest(serviceName, message, data));
    }

    public void info(Class<?> sourceClass, String message, Map<String, Object> data) {
        monitoringService.logInfo(buildRequest(resolveServiceName(sourceClass), message, data));
    }

    public void error(String message, Map<String, Object> data, Throwable throwable) {
        error("unknown", message, data, throwable);
    }

    public void error(String serviceName, String message, Map<String, Object> data, Throwable throwable) {
        monitoringService.logError(buildRequest(serviceName, message, data), throwable);
    }

    public void error(Class<?> sourceClass, String message, Map<String, Object> data, Throwable throwable) {
        monitoringService.logError(buildRequest(resolveServiceName(sourceClass), message, data), throwable);
    }

    private String resolveServiceName(Class<?> sourceClass) {
        if (sourceClass == null) {
            return "unknown";
        }
        MonitoringComponent annotation = sourceClass.getAnnotation(MonitoringComponent.class);
        String lookupKey = annotation != null ? annotation.value() : sourceClass.getSimpleName();
        String fallback = MonitoringRequestUtils.normalizeServiceName(sourceClass.getSimpleName());
        return MonitoringRequestUtils.normalizeServiceName(monitoringClientProperties.service(lookupKey, fallback));
    }

    private MonitoringLogRequest buildRequest(String serviceName, String message, Map<String, Object> data) {
        HttpServletRequest request = httpServletRequestProvider.getIfAvailable();
        String processId = request != null ? MonitoringRequestUtils.getProcessId() : null;
        String threadId = request != null ? MonitoringRequestUtils.getThreadId() : null;
        return MonitoringLogRequest.builder()
                .timestamp(ZonedDateTime.now(IST_ZONE).toInstant())
                .service(MonitoringRequestUtils.normalizeServiceName(serviceName))
                .message(message)
                .userId(MonitoringUtils.getUserId())
                .username(MonitoringUtils.getUsername())
                .tenantId(null)
                .ipAddress(MonitoringRequestUtils.getClientIp(request))
                .sessionId(MonitoringRequestUtils.getSessionId(request))
                .processId(processId)
                .threadId(threadId)
                .data(data)
                .build();
    }
}
