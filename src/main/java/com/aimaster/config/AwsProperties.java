package com.aimaster.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Type-safe, immutable AWS configuration using Java 25 record + Spring Boot 4 @ConfigurationProperties.
 */
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        String accessKeyId,
        String secretAccessKey,
        @DefaultValue("us-east-1") String region,
        S3 s3
) {
    public record S3(@DefaultValue("") String outputBucket) {}
}
