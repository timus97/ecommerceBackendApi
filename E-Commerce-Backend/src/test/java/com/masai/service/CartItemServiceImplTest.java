package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.dto.CartDTO;
import com.masai.exception.ProductNotFoundException;
import com.masai.models.CartItem;
import com.masai.models.Product;
import com.masai.models.ProductStatus;
import com.masai.repository.ProductRepository;

@DisplayName("CartItemServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class CartItemServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartItemServiceImpl cartItemService;

    private Product product;
    private CartDTO cartDTO;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductId(1);
        product.setProductName("Test Product");
        product.setPrice(100.0);
        product.setQuantity(10);
        product.setStatus(ProductStatus.AVAILABLE);

        cartDTO = new CartDTO();
        cartDTO.setProductId(1);
    }

    @Test
    @DisplayName("Should create cart item successfully")
    void testCreateItemforCart_Success() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        CartItem result = cartItemService.createItemforCart(cartDTO);

        assertNotNull(result);
        assertEquals(1, result.getCartItemQuantity());
        assertEquals(product, result.getCartProduct());
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testCreateItemforCart_ProductNotFound() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
            cartItemService.createItemforCart(cartDTO));
    }

    @Test
    @DisplayName("Should throw exception when product is out of stock")
    void testCreateItemforCart_OutOfStock() {
        product.setStatus(ProductStatus.OUTOFSTOCK);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class, () ->
            cartItemService.createItemforCart(cartDTO));
    }

    @Test
    @DisplayName("Should throw exception when product quantity is zero")
    void testCreateItemforCart_ZeroQuantity() {
        product.setQuantity(0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class, () ->
            cartItemService.createItemforCart(cartDTO));
    }
}
