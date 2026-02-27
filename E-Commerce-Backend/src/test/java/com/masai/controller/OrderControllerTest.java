package com.masai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
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

import com.masai.dto.OrderDTO;
import com.masai.models.Customer;
import com.masai.models.Order;
import com.masai.service.OrderService;

@DisplayName("OrderController Tests")
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private Order order;
    private OrderDTO orderDTO;
    private Customer customer;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderId(1);
        order.setTotal(100.0);

        orderDTO = new OrderDTO();

        customer = new Customer();
        customer.setCustomerId(1);
    }

    @Test
    @DisplayName("Should place new order")
    void testAddTheNewOrder() {
        when(orderService.saveOrder(any(OrderDTO.class), anyString())).thenReturn(order);

        ResponseEntity<Order> response = orderController.addTheNewOrder(orderDTO, "token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getOrderId());
    }

    @Test
    @DisplayName("Should get all orders")
    void testGetAllOrders() {
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(order));

        List<Order> result = orderController.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get order by ID")
    void testGetOrdersByOrderId() {
        when(orderService.getOrderByOrderId(1)).thenReturn(order);

        Order result = orderController.getOrdersByOrderId(1);

        assertNotNull(result);
        assertEquals(1, result.getOrderId());
    }

    @Test
    @DisplayName("Should cancel order by ID")
    void testCancelTheOrderByOrderId() {
        when(orderService.cancelOrderByOrderId(anyInt(), anyString())).thenReturn(order);

        Order result = orderController.cancelTheOrderByOrderId(1, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should update order")
    void testUpdateOrderByOrder() {
        when(orderService.updateOrderByOrder(any(OrderDTO.class), anyInt(), anyString())).thenReturn(order);

        ResponseEntity<Order> response = orderController.updateOrderByOrder(orderDTO, 1, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get orders by date")
    void testGetOrdersByDate() {
        when(orderService.getAllOrdersByDate(any(LocalDate.class))).thenReturn(Arrays.asList(order));

        List<Order> result = orderController.getOrdersByDate("27-02-2024");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get customer details by order ID")
    void testGetCustomerDetailsByOrderId() {
        when(orderService.getCustomerByOrderid(1)).thenReturn(customer);

        Customer result = orderController.getCustomerDetailsByOrderId(1);

        assertNotNull(result);
        assertEquals(1, result.getCustomerId());
    }
}
