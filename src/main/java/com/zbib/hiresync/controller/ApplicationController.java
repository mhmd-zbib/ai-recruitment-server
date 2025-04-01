package com.zbib.hiresync.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.zbib.hiresync.dto.ApplicationFilter;
import com.zbib.hiresync.dto.ApplicationListResponse;
import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.validator.ApplicationValidator;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing application resources. Provides endpoints for retrieving, deleting,
 * and listing applications.
 */
@RestController
@RequestMapping("/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

  private final ApplicationService applicationService;
  private final ApplicationValidator applicationValidator;

  /**
   * Retrieves an application by its ID. Access is restricted to the application owner through
   * PreAuthorize annotation.
   *
   * @param userDetailsImpl the authenticated user
   * @param id the ID of the application to retrieve
   * @return the application with HTTP status 200 (OK)
   */
  @GetMapping("/{id}")
  @PreAuthorize("@applicationValidator.isApplicationOwner(#userDetailsImpl, #id)")
  public ResponseEntity<ApplicationResponse> getApplicationById(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
    ApplicationResponse applicationResponse = applicationService.getApplicationResponseById(id);
    return ResponseEntity.ok(applicationResponse);
  }

  /**
   * Deletes an application by its ID. Access is restricted to the application owner through
   * PreAuthorize annotation.
   *
   * @param userDetailsImpl the authenticated user
   * @param id the ID of the application to delete
   * @return a success message with HTTP status 200 (OK)
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("@applicationValidator.isApplicationOwner(#userDetailsImpl, #id)")
  public ResponseEntity<String> deleteApplicationById(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
    applicationService.deleteApplicationById(id);
    return ResponseEntity.ok("Application has been deleted successfully");
  }

  /**
   * Retrieves all applications for the authenticated user with optional filtering and pagination.
   *
   * @param userDetailsImpl the authenticated user
   * @param filter criteria to filter the applications
   * @param pageable pagination information
   * @return a page of application listings with HTTP status 200 (OK)
   */
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
