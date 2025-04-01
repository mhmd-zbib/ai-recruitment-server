package com.zbib.hiresync.validator;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.zbib.hiresync.repository.JobRepository;
import com.zbib.hiresync.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JobValidator {

  private final JobRepository jobRepository;

  public boolean isJobOwner(UserDetailsImpl user, UUID jobId) {
    if (user == null || jobId == null) {
      return false;
    }
    UUID userId = user.getId();
    return jobRepository.existsByIdAndUserId(jobId, userId);
  }
}
