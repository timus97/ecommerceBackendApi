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

import com.masai.dto.CartDTO;
import com.masai.models.Cart;
import com.masai.service.CartService;

@DisplayName("CartController Tests")
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private Cart cart;
    private CartDTO cartDTO;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setCartId(1);
        cart.setCartTotal(100.0);

        cartDTO = new CartDTO();
        cartDTO.setProductId(1);
    }

    @Test
    @DisplayName("Should add product to cart")
    void testAddProductToCartHander() {
        when(cartService.addProductToCart(any(CartDTO.class), anyString())).thenReturn(cart);

        ResponseEntity<Cart> response = cartController.addProductToCartHander(cartDTO, "token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCartId());
    }

    @Test
    @DisplayName("Should get cart products")
    void testGetCartProductHandler() {
        when(cartService.getCartProduct(anyString())).thenReturn(cart);

        ResponseEntity<Cart> response = cartController.getCartProductHandler("token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should remove product from cart")
    void testRemoveProductFromCartHander() {
        when(cartService.removeProductFromCart(any(CartDTO.class), anyString())).thenReturn(cart);

        ResponseEntity<Cart> response = cartController.removeProductFromCartHander(cartDTO, "token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should clear cart")
    void testClearCartHandler() {
        when(cartService.clearCart(anyString())).thenReturn(cart);

        ResponseEntity<Cart> response = cartController.clearCartHandler("token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
