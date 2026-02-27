package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.dto.CartDTO;
import com.masai.exception.CartItemNotFound;
import com.masai.exception.CustomerNotFoundException;
import com.masai.models.Cart;
import com.masai.models.CartItem;
import com.masai.models.Customer;
import com.masai.models.Product;
import com.masai.models.UserSession;
import com.masai.repository.CartRepository;
import com.masai.repository.CustomerRepository;
import com.masai.util.TokenValidationUtil;

@DisplayName("CartServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private TokenValidationUtil tokenValidationUtil;

    @Mock
    private CartItemService cartItemService;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private UserSession userSession;
    private Customer customer;
    private Cart cart;
    private Product product;
    private CartItem cartItem;
    private CartDTO cartDTO;

    @BeforeEach
    void setUp() {
        userSession = new UserSession();
        userSession.setUserId(1);
        userSession.setToken("customer_token");

        cart = new Cart();
        cart.setCartId(1);
        cart.setCartItems(new ArrayList<>());
        cart.setCartTotal(0.0);

        product = new Product();
        product.setProductId(1);
        product.setProductName("Test Product");
        product.setPrice(100.0);

        cartItem = new CartItem();
        cartItem.setCartItemId(1);
        cartItem.setCartProduct(product);
        cartItem.setCartItemQuantity(1);

        customer = new Customer();
        customer.setCustomerId(1);
        customer.setCustomerCart(cart);

        cartDTO = new CartDTO();
        cartDTO.setProductId(1);
    }

    @Test
    @DisplayName("Should add product to empty cart successfully")
    void testAddProductToCart_EmptyCart() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(cartItemService.createItemforCart(any(CartDTO.class))).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.addProductToCart(cartDTO, "token");

        assertNotNull(result);
        assertEquals(1, result.getCartItems().size());
        assertEquals(100.0, result.getCartTotal());
    }

    @Test
    @DisplayName("Should throw exception when customer not found during add")
    void testAddProductToCart_CustomerNotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () ->
            cartService.addProductToCart(cartDTO, "token"));
    }

    @Test
    @DisplayName("Should get cart product successfully")
    void testGetCartProduct_Success() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(cartRepository.findById(1)).thenReturn(Optional.of(cart));

        Cart result = cartService.getCartProduct("token");

        assertNotNull(result);
        assertEquals(1, result.getCartId());
    }

    @Test
    @DisplayName("Should throw exception when customer not found during get")
    void testGetCartProduct_CustomerNotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () ->
            cartService.getCartProduct("token"));
    }

    @Test
    @DisplayName("Should throw exception when cart not found")
    void testGetCartProduct_CartNotFound() {
        cart.setCartId(1);
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(cartRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CartItemNotFound.class, () ->
            cartService.getCartProduct("token"));
    }

    @Test
    @DisplayName("Should remove product from cart successfully")
    void testRemoveProductFromCart_Success() {
        cart.getCartItems().add(cartItem);
        cart.setCartTotal(100.0);

        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.removeProductFromCart(cartDTO, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw exception when cart is empty during remove")
    void testRemoveProductFromCart_EmptyCart() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThrows(CartItemNotFound.class, () ->
            cartService.removeProductFromCart(cartDTO, "token"));
    }

    @Test
    @DisplayName("Should throw exception when product not in cart")
    void testRemoveProductFromCart_ProductNotInCart() {
        Product otherProduct = new Product();
        otherProduct.setProductId(2);
        
        CartItem otherItem = new CartItem();
        otherItem.setCartProduct(otherProduct);
        otherItem.setCartItemQuantity(1);
        
        cart.getCartItems().add(otherItem);

        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThrows(CartItemNotFound.class, () ->
            cartService.removeProductFromCart(cartDTO, "token"));
    }

    @Test
    @DisplayName("Should clear cart successfully")
    void testClearCart_Success() {
        cart.getCartItems().add(cartItem);
        cart.setCartTotal(100.0);

        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.clearCart("token");

        assertNotNull(result);
        assertEquals(0, result.getCartItems().size());
        assertEquals(0.0, result.getCartTotal());
    }

    @Test
    @DisplayName("Should throw exception when cart already empty")
    void testClearCart_AlreadyEmpty() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        assertThrows(CartItemNotFound.class, () ->
            cartService.clearCart("token"));
    }

    @Test
    @DisplayName("Should throw exception when customer not found during clear")
    void testClearCart_CustomerNotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () ->
            cartService.clearCart("token"));
    }
}
