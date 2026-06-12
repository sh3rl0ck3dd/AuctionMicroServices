package com.example.biddingservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BiddingServiceApplication {

  private static final Logger log = LoggerFactory.getLogger(BiddingServiceApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(BiddingServiceApplication.class, args);
    log.info("bidding-service started on port 8082");
  }
}
