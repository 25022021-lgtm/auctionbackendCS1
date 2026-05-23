package com.auction;

import java.util.logging.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Main entry point for the Auction application. */
@SpringBootApplication
public class AuctionApplication {

  private static final Logger logger = Logger.getLogger(AuctionApplication.class.getName());

  public static void main(String[] args) {
    logger.info("Starting Auction Application");
    SpringApplication.run(AuctionApplication.class, args);
  }
}
