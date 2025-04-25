package com.zbib.hiresync.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

  public boolean sendEmail(String to, String subject, String content) {
    if (log.isInfoEnabled()) {
      log.info("Sending email to {} with subject: {}", to, subject);
    }

    try {
      // Simulate email sending process
      Thread.sleep(2000);
      if (log.isInfoEnabled()) {
        log.info("Email sent successfully to {}", to);
      }
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      if (log.isErrorEnabled()) {
        log.error("Email sending interrupted to {}: {}", to, e.getMessage());
      }
      return false;
    } catch (RuntimeException e) {
      if (log.isErrorEnabled()) {
        log.error("Failed to send email to {}: {}", to, e.getMessage());
      }
      return false;
    }
  }

  public void sendBulkNotification(String[] recipients, String message) {
    if (log.isInfoEnabled()) {
      log.info("Starting bulk notification to {} recipients", recipients.length);
    }

    for (String recipient : recipients) {
      try {
        // Simulate processing delay
        Thread.sleep(500);
        if (log.isInfoEnabled()) {
          log.info("Notification sent to: {}", recipient);
        }
      } catch (InterruptedException e) {
        if (log.isErrorEnabled()) {
          log.error("Notification interrupted: {}", e.getMessage());
        }
        Thread.currentThread().interrupt();
      }
    }

    if (log.isInfoEnabled()) {
      log.info("Bulk notification completed");
    }
  }
}
