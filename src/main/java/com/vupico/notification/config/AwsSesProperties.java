package com.vupico.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.ses")
public class AwsSesProperties {

    /** When true, use Amazon SES v2 SendBulkEmail; otherwise use logging sender. */
    private boolean enabled = false;

    /** AWS region for SES (e.g. us-east-1). */
    private String region = "us-east-1";

    /** Verified From address in SES. */
    private String fromEmail = "";

    /**
     * Max recipients per {@code SendBulkEmail} call (SES quota; default 50).
     */
    private int bulkMaxEntries = 50;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public int getBulkMaxEntries() {
        return bulkMaxEntries;
    }

    public void setBulkMaxEntries(int bulkMaxEntries) {
        this.bulkMaxEntries = bulkMaxEntries;
    }
}
