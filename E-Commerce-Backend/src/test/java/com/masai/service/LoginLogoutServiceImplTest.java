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
import com.masai.repository.CustomerDao;
import com.masai.repository.SellerDao;
import com.masai.repository.SessionDao;
import com.masai.util.PasswordEncoderUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("LoginLogoutServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class LoginLogoutServiceImplTest {

    @Mock
    private SessionDao sessionDao;

    @Mock
    private CustomerDao customerDao;

    @Mock
    private SellerDao sellerDao;

    @Mock
    private PasswordEncoderUtil passwordEncoderUtil;

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
        when(customerDao.findByMobileNo("9876543210")).thenReturn(Optional.of(testCustomer));
        when(sessionDao.findByUserId(1)).thenReturn(Optional.empty());
        when(passwordEncoderUtil.matchesPassword("TestPassword123", testCustomer.getPassword())).thenReturn(true);
        when(sessionDao.save(any(UserSession.class))).thenReturn(testSession);

        UserSession result = loginLogoutService.loginCustomer(testCustomerDTO);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        verify(customerDao, times(1)).findByMobileNo("9876543210");
        verify(sessionDao, times(1)).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Should throw exception for customer not found")
    void testLoginCustomerNotFound() {
        when(customerDao.findByMobileNo("9876543210")).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> {
            loginLogoutService.loginCustomer(testCustomerDTO);
        });
    }

    @Test
    @DisplayName("Should throw exception for incorrect password")
    void testLoginCustomerIncorrectPassword() {
        when(customerDao.findByMobileNo("9876543210")).thenReturn(Optional.of(testCustomer));
        when(sessionDao.findByUserId(1)).thenReturn(Optional.empty());
        when(passwordEncoderUtil.matchesPassword(anyString(), anyString())).thenReturn(false);

        assertThrows(LoginException.class, () -> {
            loginLogoutService.loginCustomer(testCustomerDTO);
        });
    }

    @Test
    @DisplayName("Should throw exception when customer already logged in")
    void testLoginCustomerAlreadyLoggedIn() {
        when(customerDao.findByMobileNo("9876543210")).thenReturn(Optional.of(testCustomer));
        when(sessionDao.findByUserId(1)).thenReturn(Optional.of(testSession));

        assertThrows(LoginException.class, () -> {
            loginLogoutService.loginCustomer(testCustomerDTO);
        });
    }

    @Test
    @DisplayName("Should logout customer successfully")
    void testLogoutCustomerSuccess() {
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setToken("customer_abc123");

        when(sessionDao.findByToken("customer_abc123")).thenReturn(Optional.of(testSession));
        doNothing().when(sessionDao).delete(testSession);

        SessionDTO result = loginLogoutService.logoutCustomer(sessionDTO);

        assertNotNull(result);
        assertEquals("Logged out sucessfully.", result.getMessage());
        verify(sessionDao, times(1)).delete(testSession);
    }

    @Test
    @DisplayName("Should check token status successfully")
    void testCheckTokenStatusSuccess() {
        when(sessionDao.findByToken("customer_abc123")).thenReturn(Optional.of(testSession));

        assertDoesNotThrow(() -> {
            loginLogoutService.checkTokenStatus("customer_abc123");
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void testCheckTokenStatusInvalidToken() {
        when(sessionDao.findByToken("invalid_token")).thenReturn(Optional.empty());

        assertThrows(LoginException.class, () -> {
            loginLogoutService.checkTokenStatus("invalid_token");
        });
    }

    @Test
    @DisplayName("Should throw exception for expired token")
    void testCheckTokenStatusExpiredToken() {
        UserSession expiredSession = new UserSession();
        expiredSession.setSessionEndTime(LocalDateTime.now().minusHours(1));

        when(sessionDao.findByToken("expired_token")).thenReturn(Optional.of(expiredSession));
        doNothing().when(sessionDao).delete(expiredSession);

        assertThrows(LoginException.class, () -> {
            loginLogoutService.checkTokenStatus("expired_token");
        });
    }

    @Test
    @DisplayName("Should delete expired tokens")
    void testDeleteExpiredTokens() {
        UserSession expiredSession = new UserSession();
        expiredSession.setSessionEndTime(LocalDateTime.now().minusHours(1));

        when(sessionDao.findAll()).thenReturn(java.util.Arrays.asList(expiredSession));
        doNothing().when(sessionDao).delete(expiredSession);

        assertDoesNotThrow(() -> {
            loginLogoutService.deleteExpiredTokens();
        });

        verify(sessionDao, times(1)).delete(expiredSession);
    }
}
