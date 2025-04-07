package com.zbib.hiresync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal") // Cannot be final due to Spring proxying requirements
public class HireSyncApplication {

  private HireSyncApplication() {
    // Private constructor to prevent instantiation
  }

  public static void main(String[] args) {
    SpringApplication.run(HireSyncApplication.class, args);
  }
}
