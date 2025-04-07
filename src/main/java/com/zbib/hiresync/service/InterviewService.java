package com.zbib.hiresync.service;

import com.zbib.hiresync.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service class for managing interviews. */
@Service
@RequiredArgsConstructor
public class InterviewService {

  private final InterviewRepository interviewRepository;
}
