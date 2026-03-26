package com.vupico.notification.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws.ses")
public class AwsSesProperties {

    /** AWS region for SES (e.g. us-east-1). */
    @NotBlank
    private String region;

    /** Verified From address in SES. */
    @NotBlank
    private String fromEmail;

    /**
     * Max recipients per {@code SendBulkEmail} call (SES quota; configured in application.yml).
     */
    private Integer bulkMaxEntries;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public Integer getBulkMaxEntries() {
        return bulkMaxEntries;
    }

    public void setBulkMaxEntries(Integer bulkMaxEntries) {
        this.bulkMaxEntries = bulkMaxEntries;
    }
}
