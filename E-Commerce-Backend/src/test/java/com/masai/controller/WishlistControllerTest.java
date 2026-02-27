package com.masai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.masai.dto.WishlistResponseDTO;
import com.masai.models.Cart;
import com.masai.service.WishlistService;

@DisplayName("WishlistController Tests")
@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private WishlistController wishlistController;

    private WishlistResponseDTO wishlistResponseDTO;
    private Cart cart;

    @BeforeEach
    void setUp() {
        wishlistResponseDTO = new WishlistResponseDTO();
        wishlistResponseDTO.setProductId(1);
        wishlistResponseDTO.setProductName("Test Product");

        cart = new Cart();
        cart.setCartId(1);
    }

    @Test
    @DisplayName("Should add product to wishlist")
    void testAddToWishlist() {
        when(wishlistService.addToWishlist(anyInt(), anyString())).thenReturn(wishlistResponseDTO);

        ResponseEntity<WishlistResponseDTO> response = wishlistController.addToWishlist(1, "token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getProductId());
    }

    @Test
    @DisplayName("Should get wishlist")
    void testGetWishlist() {
        when(wishlistService.getWishlist(anyString())).thenReturn(Arrays.asList(wishlistResponseDTO));

        ResponseEntity<List<WishlistResponseDTO>> response = wishlistController.getWishlist("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Should remove product from wishlist")
    void testRemoveFromWishlist() {
        when(wishlistService.removeFromWishlist(anyInt(), anyString())).thenReturn("Product removed");

        ResponseEntity<Map<String, String>> response = wishlistController.removeFromWishlist(1, "token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Product removed", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Should move product to cart")
    void testMoveToCart() {
        when(wishlistService.moveToCart(anyInt(), anyString())).thenReturn(cart);

        ResponseEntity<Cart> response = wishlistController.moveToCart(1, "token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should check if product is wishlisted")
    void testIsWishlisted() {
        when(wishlistService.isWishlisted(anyInt(), anyString())).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = wishlistController.isWishlisted(1, "token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("wishlisted"));
    }
}
