package com.masai.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility class for password encoding and validation using bcrypt.
 * Provides secure password hashing with automatic salt generation.
 */
@Component
public class PasswordEncoderUtil {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Encodes a plaintext password using bcrypt with automatic salt generation.
     * 
     * @param rawPassword The plaintext password to encode
     * @return The bcrypt hashed password
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Validates a plaintext password against a bcrypt hashed password.
     * Uses constant-time comparison to prevent timing attacks.
     * 
     * @param rawPassword The plaintext password to check
     * @param encodedPassword The bcrypt hashed password to compare against
     * @return true if the password matches, false otherwise
     */
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
