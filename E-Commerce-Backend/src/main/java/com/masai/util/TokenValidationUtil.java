package com.masai.util;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.masai.exception.LoginException;
import com.masai.models.UserSession;
import com.masai.repository.SessionDao;

/**
 * Utility class for token validation and session management.
 * Centralizes token validation logic to reduce code duplication.
 */
@Component
public class TokenValidationUtil {
    
    /**
     * Validates if a token exists and belongs to a valid session.
     * 
     * @param token The session token to validate
     * @param sessionDao The DAO for accessing session data
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token, SessionDao sessionDao) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        Optional<UserSession> session = sessionDao.findByToken(token);
        return session.isPresent();
    }
    
    /**
     * Validates a token and returns the associated UserSession.
     * Throws LoginException if token is invalid or session doesn't exist.
     * 
     * @param token The session token to validate
     * @param sessionDao The DAO for accessing session data
     * @return The UserSession object if valid
     * @throws LoginException if token is null, empty, or session not found
     */
    public UserSession validateTokenAndGetSession(String token, SessionDao sessionDao) {
        if (token == null || token.isEmpty()) {
            throw new LoginException("Token cannot be null or empty");
        }
        
        UserSession userSession = sessionDao.findByToken(token)
            .orElseThrow(() -> new LoginException("Invalid or expired session token"));
        
        return userSession;
    }
}
