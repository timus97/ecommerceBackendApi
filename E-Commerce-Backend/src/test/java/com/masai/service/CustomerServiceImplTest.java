package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.dto.CustomerDTO;
import com.masai.dto.CustomerUpdateDTO;
import com.masai.dto.SessionDTO;
import com.masai.exception.CustomerException;
import com.masai.exception.CustomerNotFoundException;
import com.masai.models.Address;
import com.masai.models.CreditCard;
import com.masai.models.Customer;
import com.masai.models.Order;
import com.masai.models.UserSession;
import com.masai.repository.CustomerRepository;
import com.masai.repository.WishlistRepository;
import com.masai.util.PasswordEncoderUtil;
import com.masai.util.TokenValidationUtil;

@DisplayName("CustomerServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoginLogoutService loginService;

    @Mock
    private TokenValidationUtil tokenValidationUtil;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private PasswordEncoderUtil passwordEncoderUtil;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;
    private CustomerDTO customerDTO;
    private CustomerUpdateDTO customerUpdateDTO;
    private UserSession userSession;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setMobileNo("9876543210");
        customer.setEmailId("john@example.com");
        customer.setPassword("password123");
        customer.setAddress(new HashMap<>());
        customer.setOrders(new ArrayList<>());

        customerDTO = new CustomerDTO();
        customerDTO.setMobileId("9876543210");
        customerDTO.setPassword("password123");

        customerUpdateDTO = new CustomerUpdateDTO();
        customerUpdateDTO.setMobileNo("9876543210");
        customerUpdateDTO.setEmailId("john@example.com");

        userSession = new UserSession();
        userSession.setUserId(1);
        userSession.setToken("customer_token");
    }

    @Test
    @DisplayName("Should add customer successfully")
    void testAddCustomer_Success() {
        when(customerRepository.findByMobileNo(anyString())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encodePassword(anyString())).thenReturn("hashed_password");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.addCustomer(customer);

        assertNotNull(result);
        assertEquals(1, result.getCustomerId());
    }

    @Test
    @DisplayName("Should throw exception when customer with mobile already exists")
    void testAddCustomer_DuplicateMobile() {
        when(customerRepository.findByMobileNo(anyString())).thenReturn(Optional.of(customer));

        assertThrows(CustomerException.class, () ->
            customerService.addCustomer(customer));
    }

    @Test
    @DisplayName("Should get logged in customer details successfully")
    void testGetLoggedInCustomerDetails_Success() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        Customer result = customerService.getLoggedInCustomerDetails("token");

        assertNotNull(result);
        assertEquals(1, result.getCustomerId());
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void testGetLoggedInCustomerDetails_NotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () ->
            customerService.getLoggedInCustomerDetails("token"));
    }

    @Test
    @DisplayName("Should get all customers successfully")
    void testGetAllCustomers_Success() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findAll()).thenReturn(Arrays.asList(customer));

        List<Customer> result = customerService.getAllCustomers("token");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw exception when no customers found")
    void testGetAllCustomers_Empty() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(CustomerNotFoundException.class, () ->
            customerService.getAllCustomers("token"));
    }

    @Test
    @DisplayName("Should update customer successfully")
    void testUpdateCustomer_Success() {
        customerUpdateDTO.setFirstName("Jane");
        customerUpdateDTO.setMobileNo("9876543210");
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findByMobileNo(anyString())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.updateCustomer(customerUpdateDTO, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw exception when customer not found for update")
    void testUpdateCustomer_NotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findByMobileNo(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByEmailId(anyString())).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () ->
            customerService.updateCustomer(customerUpdateDTO, "token"));
    }

    @Test
    @DisplayName("Should update customer mobile successfully")
    void testUpdateCustomerMobileNoOrEmailId_Success() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.updateCustomerMobileNoOrEmailId(customerUpdateDTO, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should update customer password successfully")
    void testUpdateCustomerPassword_Success() {
        customerDTO.setMobileId("9876543210");
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(passwordEncoderUtil.encodePassword(anyString())).thenReturn("new_hashed_password");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(loginService.logoutCustomer(any(SessionDTO.class))).thenReturn(new SessionDTO());

        SessionDTO result = customerService.updateCustomerPassword(customerDTO, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw exception when mobile doesn't match for password update")
    void testUpdateCustomerPassword_WrongMobile() {
        customerDTO.setMobileId("9999999999");
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThrows(CustomerException.class, () ->
            customerService.updateCustomerPassword(customerDTO, "token"));
    }

    @Test
    @DisplayName("Should update address successfully")
    void testUpdateAddress_Success() {
        Address address = new Address();
        address.setCity("New City");
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.updateAddress(address, "home", "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should update credit card successfully")
    void testUpdateCreditCardDetails_Success() {
        CreditCard card = new CreditCard();
        card.setCardNumber("1234567890123456");
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.updateCreditCardDetails("token", card);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should delete customer successfully")
    void testDeleteCustomer_Success() {
        customerDTO.setMobileId("9876543210");
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(passwordEncoderUtil.matchesPassword(anyString(), anyString())).thenReturn(true);
        doNothing().when(customerRepository).delete(any(Customer.class));
        when(loginService.logoutCustomer(any(SessionDTO.class))).thenReturn(new SessionDTO());

        SessionDTO result = customerService.deleteCustomer(customerDTO, "token");

        assertNotNull(result);
        assertEquals("Deleted account and logged out successfully", result.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when verification fails for delete")
    void testDeleteCustomer_VerificationFailed() {
        customerDTO.setMobileId("9999999999");
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThrows(CustomerException.class, () ->
            customerService.deleteCustomer(customerDTO, "token"));
    }

    @Test
    @DisplayName("Should delete address successfully")
    void testDeleteAddress_Success() {
        Address address = new Address();
        customer.getAddress().put("home", address);
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.deleteAddress("home", "token");

        assertNotNull(result);
        assertFalse(result.getAddress().containsKey("home"));
    }

    @Test
    @DisplayName("Should throw exception when address type doesn't exist")
    void testDeleteAddress_NotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThrows(CustomerException.class, () ->
            customerService.deleteAddress("work", "token"));
    }

    @Test
    @DisplayName("Should get customer orders successfully")
    void testGetCustomerOrders_Success() {
        Order order = new Order();
        customer.getOrders().add(order);
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        List<Order> result = customerService.getCustomerOrders("token");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw exception when no orders found")
    void testGetCustomerOrders_Empty() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThrows(CustomerException.class, () ->
            customerService.getCustomerOrders("token"));
    }
}
