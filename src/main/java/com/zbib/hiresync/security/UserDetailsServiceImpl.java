package com.zbib.hiresync.security;

import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.logging.LoggableService;
import com.zbib.hiresync.logging.LogLevel;
import com.zbib.hiresync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LogManager.getLogger(UserDetailsServiceImpl.class);
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            // Ensure password is not null or malformed to prevent IndexOutOfBoundsException
            String password = user.getPassword();
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    password,
                    user.isEnabled(),
                    true,
                    true,
                    !user.isLocked(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        } catch (UsernameNotFoundException e) {
            logger.warn("Failed login attempt for non-existent user: {}", email);
            throw e;
        } catch (Exception e) {
            logger.error("Error loading user by username: {}, error: {}", email, e.getMessage(), e);
            throw new UsernameNotFoundException("Error loading user: " + e.getMessage(), e);
        }
    }
}