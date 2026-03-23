package com.vupico.notification.service;

import com.vupico.notification.config.AwsSesProperties;
import com.vupico.notification.tenant.TenantConfigurationEntity;
import com.vupico.notification.tenant.TenantConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.BulkEmailContent;
import software.amazon.awssdk.services.sesv2.model.BulkEmailEntry;
import software.amazon.awssdk.services.sesv2.model.BulkEmailEntryResult;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailTemplateContent;
import software.amazon.awssdk.services.sesv2.model.SendBulkEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendBulkEmailResponse;
import software.amazon.awssdk.services.sesv2.model.Template;
import software.amazon.awssdk.services.sesv2.model.TooManyRequestsException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sends email via Amazon SES v2 {@link SesV2Client#sendBulkEmail} — one API call per chunk of
 * recipients (default max 50), respecting tenant {@code email_rate_limit}.
 */
@Service
@ConditionalOnProperty(prefix = "aws.ses", name = "enabled", havingValue = "true")
public class SesEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

    private final SesV2Client sesV2Client;
    private final AwsSesProperties sesProperties;
    private final TenantConfigurationService tenantConfigurationService;
    private final TenantEmailThrottle tenantEmailThrottle;

    public SesEmailSender(
            SesV2Client sesV2Client,
            AwsSesProperties sesProperties,
            TenantConfigurationService tenantConfigurationService,
            TenantEmailThrottle tenantEmailThrottle) {
        this.sesV2Client = sesV2Client;
        this.sesProperties = sesProperties;
        this.tenantConfigurationService = tenantConfigurationService;
        this.tenantEmailThrottle = tenantEmailThrottle;
    }

    @Override
    public void send(String tenantId, String to, String subject, String body) {
        sendBatch(tenantId, List.of(to), subject, body);
    }

    @Override
    public void sendBatch(String tenantId, List<String> addresses, String subject, String body) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        String from = sesProperties.getFromEmail();
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("aws.ses.from-email must be set when aws.ses.enabled=true");
        }

        TenantConfigurationEntity cfg = tenantConfigurationService.require(tenantId);
        log.debug("SES bulk send tenant={} sesHostOrRegionHint={} recipients={}", tenantId, cfg.getEmailHost(), addresses.size());

        boolean html = looksLikeHtml(body);
        EmailTemplateContent templateContent = EmailTemplateContent.builder()
                .subject(subject)
                .html(html ? body : null)
                .text(html ? plainTextFallback(body) : body)
                .build();

        Template template = Template.builder().templateContent(templateContent).build();
        BulkEmailContent defaultContent = BulkEmailContent.builder().template(template).build();

        int max = Math.max(1, Math.min(50, sesProperties.getBulkMaxEntries()));
        List<String> list = new ArrayList<>(addresses);
        for (int i = 0; i < list.size(); i += max) {
            List<String> chunk = list.subList(i, Math.min(i + max, list.size()));
            tenantEmailThrottle.beforeSend(tenantId, chunk.size());

            List<BulkEmailEntry> entries = chunk.stream()
                    .map(addr -> BulkEmailEntry.builder()
                            .destination(Destination.builder().toAddresses(addr).build())
                            .build())
                    .toList();

            try {
                SendBulkEmailRequest request = SendBulkEmailRequest.builder()
                        .fromEmailAddress(from)
                        .defaultContent(defaultContent)
                        .bulkEmailEntries(entries)
                        .build();

                SendBulkEmailResponse response = sesV2Client.sendBulkEmail(request);
                validateResults(response, chunk);
            } catch (TooManyRequestsException e) {
                throw new EmailSendException("SES throttling: " + e.getMessage(), e);
            } catch (AwsServiceException e) {
                throw new EmailSendException("SES error: " + e.awsErrorDetails().errorMessage(), e);
            }
        }
    }

    private static void validateResults(SendBulkEmailResponse response, List<String> chunk) {
        List<BulkEmailEntryResult> results = response.bulkEmailEntryResults();
        if (results == null || results.size() != chunk.size()) {
            throw new EmailSendException("SES bulk response size mismatch: expected " + chunk.size());
        }
        List<String> failures = new ArrayList<>();
        for (int j = 0; j < results.size(); j++) {
            BulkEmailEntryResult r = results.get(j);
            String status = r.statusAsString();
            if (status == null || !"SUCCESS".equalsIgnoreCase(status)) {
                failures.add(chunk.get(j) + " -> " + status + ": " + r.error());
            }
        }
        if (!failures.isEmpty()) {
            throw new EmailSendException("SES bulk partial failure: " + failures.stream().limit(5).collect(Collectors.joining("; ")));
        }
    }

    private static boolean looksLikeHtml(String body) {
        if (body == null) {
            return false;
        }
        String s = body.toLowerCase();
        return s.contains("<html") || s.contains("<body") || s.contains("<p") || s.contains("<br");
    }

    /** Minimal fallback when body is HTML-only. */
    private static String plainTextFallback(String html) {
        return html == null ? "" : html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
