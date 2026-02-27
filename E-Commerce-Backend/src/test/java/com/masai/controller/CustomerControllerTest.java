package com.masai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

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
import com.masai.dto.CustomerUpdateDTO;
import com.masai.dto.SessionDTO;
import com.masai.models.Address;
import com.masai.models.CreditCard;
import com.masai.models.Customer;
import com.masai.models.Order;
import com.masai.service.CustomerService;

@DisplayName("CustomerController Tests")
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private Customer customer;
    private CustomerDTO customerDTO;
    private CustomerUpdateDTO customerUpdateDTO;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setFirstName("John");
        customer.setMobileNo("9876543210");

        customerDTO = new CustomerDTO();
        customerDTO.setMobileId("9876543210");
        customerDTO.setPassword("password123");

        customerUpdateDTO = new CustomerUpdateDTO();
        customerUpdateDTO.setMobileNo("9876543210");
    }

    @Test
    @DisplayName("Should get all customers")
    void testGetAllCustomersHandler() {
        when(customerService.getAllCustomers(anyString())).thenReturn(Arrays.asList(customer));

        ResponseEntity<List<Customer>> response = customerController.getAllCustomersHandler("token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Should get logged in customer details")
    void testGetLoggedInCustomerDetailsHandler() {
        when(customerService.getLoggedInCustomerDetails(anyString())).thenReturn(customer);

        ResponseEntity<Customer> response = customerController.getLoggedInCustomerDetailsHandler("token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update customer")
    void testUpdateCustomerHandler() {
        when(customerService.updateCustomer(any(CustomerUpdateDTO.class), anyString())).thenReturn(customer);

        ResponseEntity<Customer> response = customerController.updateCustomerHandler(customerUpdateDTO, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update customer mobile/email")
    void testUpdateCustomerMobileEmailHandler() {
        when(customerService.updateCustomerMobileNoOrEmailId(any(CustomerUpdateDTO.class), anyString())).thenReturn(customer);

        ResponseEntity<Customer> response = customerController.updateCustomerMobileEmailHandler(customerUpdateDTO, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update customer password")
    void testUpdateCustomerPasswordHandler() {
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setMessage("Password updated");
        when(customerService.updateCustomerPassword(any(CustomerDTO.class), anyString())).thenReturn(sessionDTO);

        ResponseEntity<SessionDTO> response = customerController.updateCustomerPasswordHandler(customerDTO, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update address")
    void testUpdateAddressHandler() {
        Address address = new Address();
        when(customerService.updateAddress(any(Address.class), anyString(), anyString())).thenReturn(customer);

        ResponseEntity<Customer> response = customerController.updateAddressHandler(address, "home", "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update credit card")
    void testUpdateCreditCardHandler() {
        CreditCard card = new CreditCard();
        when(customerService.updateCreditCardDetails(anyString(), any(CreditCard.class))).thenReturn(customer);

        ResponseEntity<Customer> response = customerController.updateCreditCardHandler("token", card);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should delete address")
    void testDeleteAddressHandler() {
        when(customerService.deleteAddress(anyString(), anyString())).thenReturn(customer);

        ResponseEntity<Customer> response = customerController.deleteAddressHandler("home", "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should delete customer")
    void testDeleteCustomerHandler() {
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setMessage("Customer deleted");
        when(customerService.deleteCustomer(any(CustomerDTO.class), anyString())).thenReturn(sessionDTO);

        ResponseEntity<SessionDTO> response = customerController.deleteCustomerHandler(customerDTO, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get customer orders")
    void testGetCustomerOrdersHandler() {
        Order order = new Order();
        when(customerService.getCustomerOrders(anyString())).thenReturn(Arrays.asList(order));

        ResponseEntity<List<Order>> response = customerController.getCustomerOrdersHandler("token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
}
