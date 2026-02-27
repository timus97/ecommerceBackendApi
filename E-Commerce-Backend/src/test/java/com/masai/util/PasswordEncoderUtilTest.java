package com.masai.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PasswordEncoderUtil Tests")
@ExtendWith(MockitoExtension.class)
class PasswordEncoderUtilTest {

    @InjectMocks
    private PasswordEncoderUtil passwordEncoderUtil;

    @Test
    @DisplayName("Should encode password successfully")
    void testEncodePassword() {
        String rawPassword = "testPassword123";
        
        String encodedPassword = passwordEncoderUtil.encodePassword(rawPassword);
        
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$")); // BCrypt prefix
    }

    @Test
    @DisplayName("Should match password correctly")
    void testMatchesPassword_Success() {
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoderUtil.encodePassword(rawPassword);
        
        boolean matches = passwordEncoderUtil.matchesPassword(rawPassword, encodedPassword);
        
        assertTrue(matches);
    }

    @Test
    @DisplayName("Should not match incorrect password")
    void testMatchesPassword_Failure() {
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword456";
        String encodedPassword = passwordEncoderUtil.encodePassword(rawPassword);
        
        boolean matches = passwordEncoderUtil.matchesPassword(wrongPassword, encodedPassword);
        
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should encode different hashes for same password")
    void testEncodePassword_UniqueHashes() {
        String rawPassword = "testPassword123";
        
        String encoded1 = passwordEncoderUtil.encodePassword(rawPassword);
        String encoded2 = passwordEncoderUtil.encodePassword(rawPassword);
        
        assertNotEquals(encoded1, encoded2); // Different salts should produce different hashes
        assertTrue(passwordEncoderUtil.matchesPassword(rawPassword, encoded1));
        assertTrue(passwordEncoderUtil.matchesPassword(rawPassword, encoded2));
    }
}
