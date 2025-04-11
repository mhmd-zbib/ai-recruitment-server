package com.zbib.hiresync.dto.builder;

import com.zbib.hiresync.dto.request.SignupRequest;
import com.zbib.hiresync.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class UserBuilder {
    
    private final PasswordEncoder passwordEncoder;

    public User buildUser(SignupRequest request) {
        return User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .enabled(true)
            .locked(false)
            .build();
    }
} 