package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.*;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.validator.ApplicationValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

  private final ApplicationService applicationService;
  private final ApplicationValidator applicationValidator;

  @GetMapping("/{id}")
  @PreAuthorize("@applicationValidator.isApplicationOwner(#userDetailsImpl, #id)")
  public ResponseEntity<ApplicationResponse> getApplicationById(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
    ApplicationResponse applicationResponse = applicationService.getApplicationResponseById(id);
    return ResponseEntity.ok(applicationResponse);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@applicationValidator.isApplicationOwner(#userDetailsImpl, #id)")
  public ResponseEntity<String> deleteApplicationById(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
    applicationService.deleteApplicationById(id);
    return ResponseEntity.ok("Application has been deleted successfully");
  }

  @GetMapping
  public ResponseEntity<Page<ApplicationListResponse>> getApplications(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
      ApplicationFilter filter,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    UUID userId = userDetailsImpl.getId();
    Page<ApplicationListResponse> applicationListPage =
        applicationService.getApplications(userId, filter, pageable);
    return ResponseEntity.ok(applicationListPage);
  }
}
