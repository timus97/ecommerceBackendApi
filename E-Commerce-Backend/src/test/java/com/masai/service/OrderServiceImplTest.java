package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.dto.CartDTO;
import com.masai.dto.OrderDTO;
import com.masai.exception.LoginException;
import com.masai.exception.OrderException;
import com.masai.models.Address;
import com.masai.models.Cart;
import com.masai.models.CartItem;
import com.masai.models.CreditCard;
import com.masai.models.Customer;
import com.masai.models.Order;
import com.masai.models.OrderStatusValues;
import com.masai.models.Product;
import com.masai.models.ProductStatus;
import com.masai.repository.OrderRepository;

@DisplayName("OrderServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Customer customer;
    private Order order;
    private OrderDTO orderDTO;
    private Cart cart;
    private Product product;
    private CartItem cartItem;
    private CreditCard creditCard;

    @BeforeEach
    void setUp() {
        creditCard = new CreditCard();
        creditCard.setCardNumber("1234567890123456");
        creditCard.setCardValidity("12/25");
        creditCard.setCardCVV("123");

        Address address = new Address();
        address.setCity("Test City");

        product = new Product();
        product.setProductId(1);
        product.setProductName("Test Product");
        product.setPrice(100.0);
        product.setQuantity(10);
        product.setStatus(ProductStatus.AVAILABLE);

        cartItem = new CartItem();
        cartItem.setCartProduct(product);
        cartItem.setCartItemQuantity(2);

        cart = new Cart();
        cart.setCartId(1);
        cart.setCartItems(new ArrayList<>(Arrays.asList(cartItem)));
        cart.setCartTotal(200.0);

        customer = new Customer();
        customer.setCustomerId(1);
        customer.setCustomerCart(cart);
        customer.setCreditCard(creditCard);
        customer.setAddress(new java.util.HashMap<>() {{ put("home", address); }});

        orderDTO = new OrderDTO();
        orderDTO.setCardNumber(creditCard);
        orderDTO.setAddressType("home");

        order = new Order();
        order.setOrderId(1);
        order.setCustomer(customer);
        order.setOrderStatus(OrderStatusValues.SUCCESS);
        order.setTotal(200.0);
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void testGetOrderByOrderId_Success() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderByOrderId(1);

        assertNotNull(result);
        assertEquals(1, result.getOrderId());
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void testGetOrderByOrderId_NotFound() {
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(OrderException.class, () ->
            orderService.getOrderByOrderId(1));
    }

    @Test
    @DisplayName("Should get all orders successfully")
    void testGetAllOrders_Success() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList(order));

        List<Order> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get orders by date successfully")
    void testGetAllOrdersByDate_Success() {
        LocalDate date = LocalDate.now();
        when(orderRepository.findByDate(date)).thenReturn(Arrays.asList(order));

        List<Order> result = orderService.getAllOrdersByDate(date);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should cancel pending order successfully")
    void testCancelOrderByOrderId_Pending() {
        order.setOrderStatus(OrderStatusValues.PENDING);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(customerService.getLoggedInCustomerDetails(anyString())).thenReturn(customer);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.cancelOrderByOrderId(1, "token");

        assertNotNull(result);
        assertEquals(OrderStatusValues.CANCELLED, result.getOrderStatus());
    }

    @Test
    @DisplayName("Should cancel success order and restore inventory")
    void testCancelOrderByOrderId_Success() {
        order.setOrderStatus(OrderStatusValues.SUCCESS);
        order.setOrdercartItems(Arrays.asList(cartItem));
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(customerService.getLoggedInCustomerDetails(anyString())).thenReturn(customer);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.cancelOrderByOrderId(1, "token");

        assertNotNull(result);
        assertEquals(OrderStatusValues.CANCELLED, result.getOrderStatus());
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled order")
    void testCancelOrderByOrderId_AlreadyCancelled() {
        order.setOrderStatus(OrderStatusValues.CANCELLED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(customerService.getLoggedInCustomerDetails(anyString())).thenReturn(customer);

        assertThrows(OrderException.class, () ->
            orderService.cancelOrderByOrderId(1, "token"));
    }

    @Test
    @DisplayName("Should throw exception when invalid token for cancel")
    void testCancelOrderByOrderId_InvalidToken() {
        Customer otherCustomer = new Customer();
        otherCustomer.setCustomerId(2);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(customerService.getLoggedInCustomerDetails(anyString())).thenReturn(otherCustomer);

        assertThrows(LoginException.class, () ->
            orderService.cancelOrderByOrderId(1, "token"));
    }

    @Test
    @DisplayName("Should get customer by order ID successfully")
    void testGetCustomerByOrderid_Success() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.getCustomerByOrderid(1)).thenReturn(customer);

        Customer result = orderService.getCustomerByOrderid(1);

        assertNotNull(result);
        assertEquals(1, result.getCustomerId());
    }

    @Test
    @DisplayName("Should throw exception when order not found for customer")
    void testGetCustomerByOrderid_NotFound() {
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(OrderException.class, () ->
            orderService.getCustomerByOrderid(1));
    }
}
