package com.zbib.hiresync.service;

import com.zbib.hiresync.dto.request.AuthRequest;
import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.dto.response.AuthResponse;

public interface AuthService {
    
    AuthResponse login(AuthRequest request);
    
    AuthResponse signup(SignupRequest request);
} 