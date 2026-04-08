package com.aimaster.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS Bedrock / S3 beans — now using the AwsProperties record
 * instead of scattered @Value annotations.
 */
@Configuration
@RequiredArgsConstructor
public class BedrockConfig {

    private final AwsProperties aws;

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(aws.accessKeyId(), aws.secretAccessKey()));
    }

    private Region region() {
        return Region.of(aws.region());
    }

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .region(region())
                .credentialsProvider(credentials())
                .build();
    }

    @Bean
    public BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient() {
        return BedrockRuntimeAsyncClient.builder()
                .region(region())
                .credentialsProvider(credentials())
                .build();
    }

    @Bean
    public BedrockClient bedrockClient() {
        return BedrockClient.builder()
                .region(region())
                .credentialsProvider(credentials())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(region())
                .credentialsProvider(credentials())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(region())
                .credentialsProvider(credentials())
                .build();
    }
}
