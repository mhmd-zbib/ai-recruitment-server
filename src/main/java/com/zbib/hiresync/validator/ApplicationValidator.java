package com.zbib.hiresync.validator;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplicationValidator {

  private final ApplicationRepository applicationRepository;

  public boolean isApplicationOwner(UserDetailsImpl user, UUID applicationId) {
    if (user == null || applicationId == null) {
      return false;
    }
    UUID userId = user.getId();
    return applicationRepository.existsByIdAndUserId(applicationId, userId);
  }
}
