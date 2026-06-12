package com.example.auctionservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuctionServiceApplication {

  private static final Logger log = LoggerFactory.getLogger(AuctionServiceApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(AuctionServiceApplication.class, args);
    log.info("auction-service started on port 8080");
  }
}
