package com.zbib.hiresync.validator;

import com.zbib.hiresync.repository.ApplicationRepository;
import com.zbib.hiresync.security.UserDetailsImpl;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationValidator {

  private final ApplicationRepository applicationRepository;

  public boolean isApplicationOwner(UserDetailsImpl user, UUID applicationId) {
    if (user == null || applicationId == null) return false;
    UUID userId = user.getId();
    return applicationRepository.existsByIdAndUserId(applicationId, userId);
  }
}
