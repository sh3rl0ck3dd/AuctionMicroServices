package com.example.auctionservice;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@ConditionalOnProperty(prefix = "auction-service.sqs", name = "enabled", havingValue = "true")
public class SqsConfig {

  @Bean
  public SqsClient sqsClient(
      @Value("${auction-service.sqs.endpoint}") String endpoint,
      @Value("${auction-service.sqs.region}") String region) {
    return SqsClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .build();
  }

  @Bean
  public String sqsQueueUrl(
      SqsClient sqsClient,
      @Value("${auction-service.sqs.queue-name}") String queueName) {
    return sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
  }
}
