package com.masai.util;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.masai.exception.LoginException;
import com.masai.models.UserSession;
import com.masai.repository.SessionRepository;

/**
 * Utility class for token validation and session management.
 * Centralizes token validation logic to reduce code duplication.
 */
@Component
public class TokenValidationUtil {
    
    public static final String CUSTOMER_PREFIX = "customer_";
    public static final String SELLER_PREFIX = "seller_";
    public static final long SESSION_DURATION_HOURS = 1;

    @Autowired
    private SessionRepository sessionRepository;
    
    /**
     * Validates if a token exists and belongs to a valid session.
     * 
     * @param token The session token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || (token = token.trim()).isEmpty()) {
            return false;
        }
        
        Optional<UserSession> session = sessionRepository.findByToken(token);
        return session.isPresent() && !session.get().getSessionEndTime().isBefore(LocalDateTime.now());
    }
    
    /**
     * Validates a token and returns the associated UserSession.
     * Throws LoginException if token is invalid or session doesn't exist.
     * 
     * @param token The session token to validate
     * @return The UserSession object if valid
     * @throws LoginException if token is null, empty, or session not found
     */
    public UserSession validateTokenAndGetSession(String token) {
        if (token == null || (token = token.trim()).isEmpty()) {
            throw new LoginException("Token cannot be null or empty");
        }
        
        UserSession userSession = sessionRepository.findByToken(token)
            .orElseThrow(() -> new LoginException("Invalid or expired session token"));
        
        if (userSession.getSessionEndTime().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(userSession);
            throw new LoginException("Session expired. Login Again");
        }
        
        return userSession;
    }

    /**
     * Validates a customer token and returns the associated UserSession.
     * 
     * @param token The session token to validate
     * @return The UserSession object if valid
     * @throws LoginException if token is invalid, not a customer token, or session not found
     */
    public UserSession validateCustomerToken(String token) {
        if (token == null || (token = token.trim()).isEmpty()) {
             throw new LoginException("Invalid customer token");
        }
        if (!token.startsWith(CUSTOMER_PREFIX)) {
             throw new LoginException("Invalid customer token");
        }
        return validateTokenAndGetSession(token);
    }

    /**
     * Validates a seller token and returns the associated UserSession.
     * 
     * @param token The session token to validate
     * @return The UserSession object if valid
     * @throws LoginException if token is invalid, not a seller token, or session not found
     */
    public UserSession validateSellerToken(String token) {
        if (token == null || (token = token.trim()).isEmpty()) {
             throw new LoginException("Invalid seller token");
        }
        if (!token.startsWith(SELLER_PREFIX)) {
             throw new LoginException("Invalid seller token");
        }
        return validateTokenAndGetSession(token);
    }
}
