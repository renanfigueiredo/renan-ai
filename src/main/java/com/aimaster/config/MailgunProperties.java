package com.aimaster.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mailgun HTTP API configuration bound as an immutable Java 25 record.
 */
@ConfigurationProperties(prefix = "mailgun")
public record MailgunProperties(
        String apiKey,
        String domain
) {}
