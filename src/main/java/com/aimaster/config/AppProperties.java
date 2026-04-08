package com.aimaster.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application-level configuration bound as an immutable Java 25 record.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:8080") String baseUrl,
        @DefaultValue("evj.inven@gmail.com") String adminEmail,
        @DefaultValue("EVJ AI <postmaster@evj.app.br>") String mailFrom
) {}
