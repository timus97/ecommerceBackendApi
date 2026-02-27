package com.masai.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.exception.LoginException;
import com.masai.models.UserSession;
import com.masai.repository.SessionRepository;

@DisplayName("TokenValidationUtil Tests")
@ExtendWith(MockitoExtension.class)
class TokenValidationUtilTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private TokenValidationUtil tokenValidationUtil;

    private UserSession validSession;
    private UserSession expiredSession;

    @BeforeEach
    void setUp() {
        validSession = new UserSession();
        validSession.setToken("customer_test_token");
        validSession.setUserId(1);
        validSession.setSessionEndTime(LocalDateTime.now().plusHours(1));

        expiredSession = new UserSession();
        expiredSession.setToken("customer_expired_token");
        expiredSession.setUserId(2);
        expiredSession.setSessionEndTime(LocalDateTime.now().minusHours(1));
    }

    @Test
    @DisplayName("Should validate token successfully")
    void testValidateToken_Success() {
        when(sessionRepository.findByToken("customer_test_token")).thenReturn(Optional.of(validSession));

        boolean result = tokenValidationUtil.validateToken("customer_test_token");

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for null token")
    void testValidateToken_Null() {
        boolean result = tokenValidationUtil.validateToken(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for empty token")
    void testValidateToken_Empty() {
        boolean result = tokenValidationUtil.validateToken("   ");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for expired session")
    void testValidateToken_Expired() {
        when(sessionRepository.findByToken("customer_expired_token")).thenReturn(Optional.of(expiredSession));

        boolean result = tokenValidationUtil.validateToken("customer_expired_token");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for non-existent token")
    void testValidateToken_NotFound() {
        when(sessionRepository.findByToken("invalid_token")).thenReturn(Optional.empty());

        boolean result = tokenValidationUtil.validateToken("invalid_token");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should validate token and get session successfully")
    void testValidateTokenAndGetSession_Success() {
        when(sessionRepository.findByToken("customer_test_token")).thenReturn(Optional.of(validSession));

        UserSession result = tokenValidationUtil.validateTokenAndGetSession("customer_test_token");

        assertNotNull(result);
        assertEquals(1, result.getUserId());
    }

    @Test
    @DisplayName("Should throw exception for null token")
    void testValidateTokenAndGetSession_Null() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateTokenAndGetSession(null));
    }

    @Test
    @DisplayName("Should throw exception for empty token")
    void testValidateTokenAndGetSession_Empty() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateTokenAndGetSession("   "));
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void testValidateTokenAndGetSession_Invalid() {
        when(sessionRepository.findByToken("invalid_token")).thenReturn(Optional.empty());

        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateTokenAndGetSession("invalid_token"));
    }

    @Test
    @DisplayName("Should throw exception for expired session and delete it")
    void testValidateTokenAndGetSession_Expired() {
        when(sessionRepository.findByToken("customer_expired_token")).thenReturn(Optional.of(expiredSession));
        doNothing().when(sessionRepository).delete(expiredSession);

        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateTokenAndGetSession("customer_expired_token"));

        verify(sessionRepository).delete(expiredSession);
    }

    @Test
    @DisplayName("Should validate customer token successfully")
    void testValidateCustomerToken_Success() {
        when(sessionRepository.findByToken("customer_test_token")).thenReturn(Optional.of(validSession));

        UserSession result = tokenValidationUtil.validateCustomerToken("customer_test_token");

        assertNotNull(result);
        assertEquals(1, result.getUserId());
    }

    @Test
    @DisplayName("Should throw exception for null customer token")
    void testValidateCustomerToken_Null() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateCustomerToken(null));
    }

    @Test
    @DisplayName("Should throw exception for empty customer token")
    void testValidateCustomerToken_Empty() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateCustomerToken("   "));
    }

    @Test
    @DisplayName("Should throw exception for non-customer token")
    void testValidateCustomerToken_NotCustomer() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateCustomerToken("seller_test_token"));
    }

    @Test
    @DisplayName("Should validate seller token successfully")
    void testValidateSellerToken_Success() {
        UserSession sellerSession = new UserSession();
        sellerSession.setToken("seller_test_token");
        sellerSession.setUserId(1);
        sellerSession.setSessionEndTime(LocalDateTime.now().plusHours(1));

        when(sessionRepository.findByToken("seller_test_token")).thenReturn(Optional.of(sellerSession));

        UserSession result = tokenValidationUtil.validateSellerToken("seller_test_token");

        assertNotNull(result);
        assertEquals(1, result.getUserId());
    }

    @Test
    @DisplayName("Should throw exception for null seller token")
    void testValidateSellerToken_Null() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateSellerToken(null));
    }

    @Test
    @DisplayName("Should throw exception for empty seller token")
    void testValidateSellerToken_Empty() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateSellerToken("   "));
    }

    @Test
    @DisplayName("Should throw exception for non-seller token")
    void testValidateSellerToken_NotSeller() {
        assertThrows(LoginException.class, () ->
            tokenValidationUtil.validateSellerToken("customer_test_token"));
    }
}
