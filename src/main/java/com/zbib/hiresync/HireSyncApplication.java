package com.zbib.hiresync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.experimental.UtilityClass;

@SpringBootApplication
@UtilityClass
public class HireSyncApplication {

  public static void main(String[] args) {
    SpringApplication.run(HireSyncApplication.class, args);
  }
}
