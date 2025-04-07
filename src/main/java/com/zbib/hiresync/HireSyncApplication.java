package com.zbib.hiresync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HireSyncApplication {

  private HireSyncApplication() {
    // Private constructor to prevent instantiation
  }

  public static void main(String[] args) {
    SpringApplication.run(HireSyncApplication.class, args);
  }
}
