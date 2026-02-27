package com.masai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.masai.dto.CustomerDTO;
import com.masai.dto.SellerDTO;
import com.masai.dto.SessionDTO;
import com.masai.models.Customer;
import com.masai.models.Seller;
import com.masai.models.UserSession;
import com.masai.service.CustomerService;
import com.masai.service.LoginLogoutService;
import com.masai.service.SellerService;

@DisplayName("LoginController Tests")
@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private LoginLogoutService loginService;

    @Mock
    private SellerService sellerService;

    @InjectMocks
    private LoginController loginController;

    private Customer customer;
    private Seller seller;
    private CustomerDTO customerDTO;
    private SellerDTO sellerDTO;
    private UserSession userSession;
    private SessionDTO sessionDTO;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setFirstName("John");
        customer.setMobileNo("9876543210");

        seller = new Seller();
        seller.setSellerId(1);
        seller.setFirstName("Jane");
        seller.setMobile("9876543211");

        customerDTO = new CustomerDTO();
        customerDTO.setMobileId("9876543210");
        customerDTO.setPassword("password123");

        sellerDTO = new SellerDTO();
        sellerDTO.setMobile("9876543211");
        sellerDTO.setPassword("password123");

        userSession = new UserSession();
        userSession.setToken("customer_token");
        userSession.setUserId(1);

        sessionDTO = new SessionDTO();
        sessionDTO.setToken("customer_token");
    }

    @Test
    @DisplayName("Should register customer successfully")
    void testRegisterAccountHandler() {
        when(customerService.addCustomer(any(Customer.class))).thenReturn(customer);

        ResponseEntity<Customer> response = loginController.registerAccountHandler(customer);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCustomerId());
    }

    @Test
    @DisplayName("Should login customer successfully")
    void testLoginCustomerHandler() {
        when(loginService.loginCustomer(any(CustomerDTO.class))).thenReturn(userSession);

        ResponseEntity<UserSession> response = loginController.loginCustomerHandler(customerDTO);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("customer_token", response.getBody().getToken());
    }

    @Test
    @DisplayName("Should logout customer successfully")
    void testLogoutCustomerHandler() {
        SessionDTO resultDTO = new SessionDTO();
        resultDTO.setMessage("Logged out successfully");
        when(loginService.logoutCustomer(any(SessionDTO.class))).thenReturn(resultDTO);

        ResponseEntity<SessionDTO> response = loginController.logoutCustomerHandler(sessionDTO);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should register seller successfully")
    void testRegisterSellerAccountHandler() {
        when(sellerService.addSeller(any(Seller.class))).thenReturn(seller);

        ResponseEntity<Seller> response = loginController.registerSellerAccountHandler(seller);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSellerId());
    }

    @Test
    @DisplayName("Should login seller successfully")
    void testLoginSellerHandler() {
        UserSession sellerSession = new UserSession();
        sellerSession.setToken("seller_token");
        sellerSession.setUserId(1);
        when(loginService.loginSeller(any(SellerDTO.class))).thenReturn(sellerSession);

        ResponseEntity<UserSession> response = loginController.loginSellerHandler(sellerDTO);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("seller_token", response.getBody().getToken());
    }

    @Test
    @DisplayName("Should logout seller successfully")
    void testLogoutSellerHandler() {
        SessionDTO resultDTO = new SessionDTO();
        resultDTO.setMessage("Logged out successfully");
        when(loginService.logoutSeller(any(SessionDTO.class))).thenReturn(resultDTO);

        ResponseEntity<SessionDTO> response = loginController.logoutSellerHandler(sessionDTO);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
