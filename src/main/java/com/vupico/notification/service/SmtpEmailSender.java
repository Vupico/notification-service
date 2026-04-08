package com.vupico.notification.service;

import com.vupico.notification.tenant.TenantConfigurationEntity;
import com.vupico.notification.tenant.TenantConfigurationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

@Service
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final MailProperties mailProperties;
    private final String fromEmail;
    private final JavaMailSender mailSender;
    private final TenantConfigurationService tenantConfigurationService;
    private final TenantEmailThrottle tenantEmailThrottle;

    public SmtpEmailSender(
            MailProperties mailProperties,
            @Value("${spring.mail.from:}") String fromEmail,
            JavaMailSender mailSender,
            TenantConfigurationService tenantConfigurationService,
            TenantEmailThrottle tenantEmailThrottle) {
        this.mailProperties = mailProperties;
        this.fromEmail = fromEmail;
        this.mailSender = mailSender;
        this.tenantConfigurationService = tenantConfigurationService;
        this.tenantEmailThrottle = tenantEmailThrottle;
    }

    @Override
    public void sendBatch(
            String tenantId,
            List<String> addresses,
            String subject,
            String body,
            String fromDisplayName,
            boolean highImportance) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }

        TenantConfigurationEntity cfg = tenantConfigurationService.require(tenantId);
        log.debug("SMTP send tenant={} hostHint={} recipients={}", tenantId, cfg.getEmailHost(), addresses.size());

        tenantEmailThrottle.beforeSend(tenantId, addresses.size());
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            applyFrom(helper, fromDisplayName);
            helper.setTo(addresses.toArray(String[]::new));
            helper.setSubject(subject == null ? "" : subject);
            helper.setText(body == null ? "" : body, looksLikeHtml(body));
            if (highImportance) {
                applyHighImportance(helper);
            }
            mailSender.send(msg);
        } catch (Exception e) {
            Integer syntheticStatus = isTransientConnectivityFailure(e) ? 503 : null;
            throw new EmailSendException("SMTP send failed: " + e.getMessage(), e, syntheticStatus);
        }
    }

    private static boolean isTransientConnectivityFailure(Throwable t) {
        while (t != null) {
            if (t instanceof UnknownHostException
                    || t instanceof ConnectException
                    || t instanceof SocketTimeoutException) {
                return true;
            }
            String name = t.getClass().getName();
            // Avoid depending directly on Angus/Jakarta mail impl types.
            if (name.endsWith(".MailConnectException") || name.endsWith(".MailSendException")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static void applyHighImportance(MimeMessageHelper helper) throws MessagingException {
        helper.setPriority(1);
        helper.getMimeMessage().setHeader("Importance", "high");
    }

    private void applyFrom(MimeMessageHelper helper, String fromDisplayName) throws Exception {
        String address = resolveFromEmail();
        if (fromDisplayName != null && !fromDisplayName.isBlank()) {
            helper.setFrom(new InternetAddress(address, fromDisplayName.trim(), StandardCharsets.UTF_8.name()));
        } else {
            helper.setFrom(address);
        }
    }

    private String resolveFromEmail() {
        String actualFrom = fromEmail;
        if (actualFrom == null || actualFrom.isBlank()) {
            actualFrom = mailProperties.getUsername();
        }
        if (actualFrom == null || actualFrom.isBlank()) {
            throw new IllegalStateException(
                    "Missing from email. Configure `spring.mail.from` (preferred) or `spring.mail.username` as a valid sender.");
        }
        return actualFrom;
    }

    private static boolean looksLikeHtml(String body) {
        if (body == null) {
            return false;
        }
        String s = body.toLowerCase();
        return s.contains("<html") || s.contains("<body") || s.contains("<p") || s.contains("<br");
    }
}
