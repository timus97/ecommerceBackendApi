package com.masai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import com.masai.exception.CustomerNotFoundException;
import com.masai.exception.LoginException;
import com.masai.exception.SellerNotFoundException;
import com.masai.models.Customer;
import com.masai.dto.CustomerDTO;
import com.masai.models.Seller;
import com.masai.dto.SellerDTO;
import com.masai.dto.SessionDTO;
import com.masai.models.UserSession;
import com.masai.repository.CustomerRepository;
import com.masai.repository.SellerRepository;
import com.masai.repository.SessionRepository;
import com.masai.util.PasswordEncoderUtil;
import com.masai.util.TokenValidationUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("LoginLogoutServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class LoginLogoutServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoderUtil passwordEncoderUtil;

    @Mock
    private TokenValidationUtil tokenValidationUtil;

    @InjectMocks
    private LoginLogoutServiceImpl loginLogoutService;

    private Customer testCustomer;
    private CustomerDTO testCustomerDTO;
    private UserSession testSession;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(1);
        testCustomer.setMobileNo("9876543210");
        testCustomer.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86AGR0Iq/3e");

        testCustomerDTO = new CustomerDTO();
        testCustomerDTO.setMobileId("9876543210");
        testCustomerDTO.setPassword("TestPassword123");

        testSession = new UserSession();
        testSession.setUserId(1);
        testSession.setToken("customer_abc123");
        testSession.setSessionStartTime(LocalDateTime.now());
        testSession.setSessionEndTime(LocalDateTime.now().plusHours(1));
    }

    @Test
    @DisplayName("Should login customer successfully")
    void testLoginCustomerSuccess() {
        when(customerRepository.findByMobileNo("9876543210")).thenReturn(Optional.of(testCustomer));
        when(sessionRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(passwordEncoderUtil.matchesPassword("TestPassword123", testCustomer.getPassword())).thenReturn(true);
        when(sessionRepository.save(any(UserSession.class))).thenReturn(testSession);

        UserSession result = loginLogoutService.loginCustomer(testCustomerDTO);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        verify(customerRepository, times(1)).findByMobileNo("9876543210");
        verify(sessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Should throw exception for customer not found")
    void testLoginCustomerNotFound() {
        when(customerRepository.findByMobileNo("9876543210")).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> {
            loginLogoutService.loginCustomer(testCustomerDTO);
        });
    }

    @Test
    @DisplayName("Should throw exception for incorrect password")
    void testLoginCustomerIncorrectPassword() {
        when(customerRepository.findByMobileNo("9876543210")).thenReturn(Optional.of(testCustomer));
        when(sessionRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(passwordEncoderUtil.matchesPassword(anyString(), anyString())).thenReturn(false);

        assertThrows(LoginException.class, () -> {
            loginLogoutService.loginCustomer(testCustomerDTO);
        });
    }

    @Test
    @DisplayName("Should throw exception when customer already logged in")
    void testLoginCustomerAlreadyLoggedIn() {
        when(customerRepository.findByMobileNo("9876543210")).thenReturn(Optional.of(testCustomer));
        when(sessionRepository.findByUserId(1)).thenReturn(Optional.of(testSession));

        assertThrows(LoginException.class, () -> {
            loginLogoutService.loginCustomer(testCustomerDTO);
        });
    }

    @Test
    @DisplayName("Should logout customer successfully")
    void testLogoutCustomerSuccess() {
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setToken("customer_abc123");

        when(tokenValidationUtil.validateCustomerToken("customer_abc123")).thenReturn(testSession);
        doNothing().when(sessionRepository).delete(testSession);

        SessionDTO result = loginLogoutService.logoutCustomer(sessionDTO);

        assertNotNull(result);
        assertEquals("Logged out sucessfully.", result.getMessage());
        verify(sessionRepository, times(1)).delete(testSession);
    }

    @Test
    @DisplayName("Should check token status successfully")
    void testCheckTokenStatusSuccess() {
        when(tokenValidationUtil.validateTokenAndGetSession("customer_abc123")).thenReturn(testSession);

        assertDoesNotThrow(() -> {
            loginLogoutService.checkTokenStatus("customer_abc123");
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void testCheckTokenStatusInvalidToken() {
        when(tokenValidationUtil.validateTokenAndGetSession("invalid_token"))
            .thenThrow(new LoginException("Invalid or expired session token"));

        assertThrows(LoginException.class, () -> {
            loginLogoutService.checkTokenStatus("invalid_token");
        });
    }

    @Test
    @DisplayName("Should throw exception for expired token")
    void testCheckTokenStatusExpiredToken() {
        when(tokenValidationUtil.validateTokenAndGetSession("expired_token"))
            .thenThrow(new LoginException("Session expired. Login Again"));

        assertThrows(LoginException.class, () -> {
            loginLogoutService.checkTokenStatus("expired_token");
        });
    }

    @Test
    @DisplayName("Should delete expired tokens")
    void testDeleteExpiredTokens() {
        UserSession expiredSession = new UserSession();
        expiredSession.setSessionEndTime(LocalDateTime.now().minusHours(1));

        when(sessionRepository.findAll()).thenReturn(java.util.Arrays.asList(expiredSession));
        doNothing().when(sessionRepository).delete(expiredSession);

        assertDoesNotThrow(() -> {
            loginLogoutService.deleteExpiredTokens();
        });

        verify(sessionRepository, times(1)).delete(expiredSession);
    }
}
