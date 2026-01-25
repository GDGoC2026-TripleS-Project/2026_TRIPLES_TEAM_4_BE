package com.gdg.unimatebackend.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class S3Config {

    /**
     * ✅ prod: EC2 IAM Role(Instance Profile) 자동 사용
     * - access-key/secret-key를 yml/env에 둘 필요 없음
     */
    @Bean
    @Profile("prod")
    public AmazonS3 amazonS3Prod(
            @Value("${aws.s3.region}") String region
    ) {
        // DefaultAWSCredentialsProviderChain:
        // EC2/ECS IAM Role -> env -> system properties -> profile file 순서로 자동 탐색
        AWSCredentialsProvider provider = DefaultAWSCredentialsProviderChain.getInstance();

        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(provider)
                .build();
    }

    /**
     * ✅ local: 키로 S3 접근 (로컬 PC에는 IAM Role이 없으니까)
     * - AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY 로 주입하거나
     * - application-local.yml에 aws.s3.access-key/secret-key로 주입
     */
    @Bean
    @Profile("local")
    public AmazonS3 amazonS3Local(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey
    ) {
        BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .build();
    }
}
