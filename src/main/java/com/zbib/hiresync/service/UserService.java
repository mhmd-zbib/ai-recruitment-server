package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.exception.AuthException;
import com.zbib.hiresync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findByUsernameOrThrow(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> AuthException.userNotFound(username));
    }
}