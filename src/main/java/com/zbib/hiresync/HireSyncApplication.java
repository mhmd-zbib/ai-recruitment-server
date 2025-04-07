package com.zbib.hiresync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for HireSync.
 */
@SpringBootApplication
public final class HireSyncApplication {

    private HireSyncApplication() {
        // Private constructor to prevent instantiation
    }

    /**
     * Main method to start the application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(HireSyncApplication.class, args);
    }
}
