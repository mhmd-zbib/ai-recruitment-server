package com.zbib.hiresync.controller;

import com.zbib.hiresync.dto.ApplicationResponse;
import com.zbib.hiresync.security.UserDetailsImpl;
import com.zbib.hiresync.service.ApplicationService;
import com.zbib.hiresync.validator.ApplicationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/applications/{id}")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationValidator applicationValidator;

    @GetMapping
    @PreAuthorize("@applicationValidator.isApplicationOwner(#userDetailsImpl, #id)")
    public ResponseEntity<ApplicationResponse> getApplicationById(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
        ApplicationResponse applicationResponse = applicationService.getApplicationResponseById(id);
        return ResponseEntity.ok(applicationResponse);
    }

    @DeleteMapping
    @PreAuthorize("@applicationValidator.isApplicationOwner(#userDetailsImpl, #id)")
    public ResponseEntity<String> deleteApplicationById(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, @PathVariable UUID id) {
        applicationService.deleteApplicationById(id);
        return ResponseEntity.ok("Application has been deleted successfully");
    }
}