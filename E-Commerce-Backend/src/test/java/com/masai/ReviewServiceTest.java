package com.masai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.exception.LoginException;
import com.masai.exception.ReviewException;
import com.masai.models.Customer;
import com.masai.models.Product;
import com.masai.models.Review;
import com.masai.dto.ReviewRequestDTO;
import com.masai.dto.ReviewResponseDTO;
import com.masai.models.UserSession;
import com.masai.repository.CustomerRepository;
import com.masai.repository.ProductRepository;
import com.masai.repository.ReviewRepository;
import com.masai.service.ReviewServiceImpl;
import com.masai.util.TokenValidationUtil;

/**
 * Unit tests for ReviewServiceImpl (mocks for dependencies).
 * Covers core methods and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TokenValidationUtil tokenValidationUtil;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Customer customer;
    private Product product;
    private ReviewRequestDTO requestDTO;
    private UserSession userSession;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setMobileNo("9999999999");

        product = new Product();
        product.setProductId(1);
        product.setProductName("Test Product");

        requestDTO = new ReviewRequestDTO();
        requestDTO.setProductId(1);
        requestDTO.setRating(4);
        requestDTO.setTitle("Great Product");
        requestDTO.setComment("Very good quality.");

        userSession = new UserSession();
        userSession.setUserId(1);
        userSession.setToken("customer_testtoken");
    }

    @Test
    void testAddReview_Success() throws Exception {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByCustomerAndProduct(any(), any())).thenReturn(false);

        Review savedReview = new Review();
        savedReview.setId(10L);
        savedReview.setRating(4);
        savedReview.setCustomer(customer);
        savedReview.setProduct(product);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponseDTO response = reviewService.addReview(requestDTO, "customer_testtoken");

        assertNotNull(response);
        assertEquals(4, response.getRating());
        assertEquals(10L, response.getId());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void testAddReview_DuplicateReview_ThrowsException() {
        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByCustomerAndProduct(any(), any())).thenReturn(true);

        assertThrows(ReviewException.class, () -> 
            reviewService.addReview(requestDTO, "customer_testtoken"));
    }

    @Test
    void testAddReview_InvalidRating_ThrowsException() {
        requestDTO.setRating(6);  // Invalid >5

        when(tokenValidationUtil.validateCustomerToken(anyString())).thenReturn(userSession);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByCustomerAndProduct(any(), any())).thenReturn(false);

        assertThrows(ReviewException.class, () ->
            reviewService.addReview(requestDTO, "customer_testtoken"));
    }

    // Additional edge case tests (duplicate, invalid rating, etc.) follow similar mocking pattern...
}
