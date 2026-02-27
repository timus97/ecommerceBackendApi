package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.dto.WishlistResponseDTO;
import com.masai.exception.CustomerNotFoundException;
import com.masai.exception.ProductNotFoundException;
import com.masai.exception.WishlistException;
import com.masai.models.Cart;
import com.masai.models.CartItem;
import com.masai.models.Customer;
import com.masai.models.Product;
import com.masai.models.UserSession;
import com.masai.models.Wishlist;
import com.masai.models.WishlistItem;
import com.masai.repository.CartRepository;
import com.masai.repository.CustomerRepository;
import com.masai.repository.ProductRepository;
import com.masai.repository.WishlistItemRepository;
import com.masai.repository.WishlistRepository;
import com.masai.util.TokenValidationUtil;

@DisplayName("WishlistServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private TokenValidationUtil tokenValidationUtil;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemService cartItemService;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private UserSession userSession;
    private Customer customer;
    private Wishlist wishlist;
    private Product product;
    private WishlistItem wishlistItem;

    @BeforeEach
    void setUp() {
        userSession = new UserSession();
        userSession.setUserId(1);
        userSession.setToken("customer_token");

        wishlist = new Wishlist();
        wishlist.setWishlistId(1);

        product = new Product();
        product.setProductId(1);
        product.setProductName("Test Product");
        product.setPrice(100.0);

        wishlistItem = new WishlistItem();
        wishlistItem.setWishlistItemId(1);
        wishlistItem.setProduct(product);
        wishlistItem.setAddedAt(LocalDateTime.now());
        wishlistItem.setWishlist(wishlist);

        Cart cart = new Cart();
        cart.setCartId(1);
        cart.setCartItems(new java.util.ArrayList<>());
        cart.setCartTotal(0.0);

        customer = new Customer();
        customer.setCustomerId(1);
        customer.setCustomerWishlist(wishlist);
        customer.setCustomerCart(cart);
    }

    @Test
    @DisplayName("Should add product to wishlist successfully")
    void testAddToWishlist_Success() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.empty());
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(wishlistItem);

        WishlistResponseDTO result = wishlistService.addToWishlist(1, "token");

        assertNotNull(result);
        assertEquals(1, result.getProductId());
    }

    @Test
    @DisplayName("Should throw exception when product already in wishlist")
    void testAddToWishlist_Duplicate() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.of(wishlistItem));

        assertThrows(WishlistException.class, () ->
            wishlistService.addToWishlist(1, "token"));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testAddToWishlist_ProductNotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.empty());
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
            wishlistService.addToWishlist(1, "token"));
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void testAddToWishlist_CustomerNotFound() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () ->
            wishlistService.addToWishlist(1, "token"));
    }

    @Test
    @DisplayName("Should get wishlist successfully")
    void testGetWishlist_Success() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdOrderByAddedAtDesc(anyInt()))
            .thenReturn(Arrays.asList(wishlistItem));

        var result = wishlistService.getWishlist("token");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should remove product from wishlist successfully")
    void testRemoveFromWishlist_Success() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.of(wishlistItem));
        doNothing().when(wishlistItemRepository).delete(any(WishlistItem.class));

        String result = wishlistService.removeFromWishlist(1, "token");

        assertEquals("Product removed from wishlist successfully", result);
    }

    @Test
    @DisplayName("Should throw exception when product not in wishlist for removal")
    void testRemoveFromWishlist_NotInWishlist() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.empty());

        assertThrows(WishlistException.class, () ->
            wishlistService.removeFromWishlist(1, "token"));
    }

    @Test
    @DisplayName("Should move product to cart successfully")
    void testMoveToCart_Success() {
        CartItem cartItem = new CartItem();
        cartItem.setCartProduct(product);
        cartItem.setCartItemQuantity(1);

        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.of(wishlistItem));
        doNothing().when(wishlistItemRepository).delete(any(WishlistItem.class));
        when(cartItemService.createItemforCart(any())).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(customer.getCustomerCart());

        Cart result = wishlistService.moveToCart(1, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw exception when product not in wishlist for move")
    void testMoveToCart_NotInWishlist() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.empty());

        assertThrows(WishlistException.class, () ->
            wishlistService.moveToCart(1, "token"));
    }

    @Test
    @DisplayName("Should check if product is wishlisted - true")
    void testIsWishlisted_True() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.of(wishlistItem));

        boolean result = wishlistService.isWishlisted(1, "token");

        assertTrue(result);
    }

    @Test
    @DisplayName("Should check if product is wishlisted - false")
    void testIsWishlisted_False() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistItemRepository.findByWishlist_WishlistIdAndProduct_ProductId(anyInt(), anyInt()))
            .thenReturn(Optional.empty());

        boolean result = wishlistService.isWishlisted(1, "token");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should lazily initialize wishlist when null")
    void testResolveCustomer_LazyInitWishlist() {
        customer.setCustomerWishlist(null);
        Wishlist newWishlist = new Wishlist();
        newWishlist.setWishlistId(2);
        
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(newWishlist);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(wishlistItemRepository.findByWishlist_WishlistIdOrderByAddedAtDesc(anyInt()))
            .thenReturn(Collections.emptyList());

        var result = wishlistService.getWishlist("token");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
