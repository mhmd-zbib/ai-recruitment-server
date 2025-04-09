package com.zbib.hiresync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HireSyncApplication {

  /**
   * Private constructor to prevent instantiation of this class.
   * This addresses the PMD "UseUtilityClass" warning.
   */
  private HireSyncApplication() {
    // This class should not be instantiated
  }

  public static void main(String[] args) {
    SpringApplication.run(HireSyncApplication.class, args);
  }
}
