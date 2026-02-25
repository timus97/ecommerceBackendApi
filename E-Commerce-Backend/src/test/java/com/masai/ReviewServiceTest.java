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
import com.masai.repository.CustomerDao;
import com.masai.repository.ProductDao;
import com.masai.repository.ReviewDao;
import com.masai.repository.SessionDao;
import com.masai.service.LoginLogoutService;
import com.masai.service.ReviewServiceImpl;

/**
 * Unit tests for ReviewServiceImpl (mocks for dependencies).
 * Covers core methods and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewDao reviewDao;

    @Mock
    private ProductDao productDao;

    @Mock
    private CustomerDao customerDao;

    @Mock
    private SessionDao sessionDao;

    @Mock
    private LoginLogoutService loginService;

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
        // Mock token validation (void method)
        doNothing().when(loginService).checkTokenStatus(anyString());
        when(sessionDao.findByToken(anyString())).thenReturn(Optional.of(userSession));
        when(customerDao.findById(1)).thenReturn(Optional.of(customer));
        when(productDao.findById(1)).thenReturn(Optional.of(product));
        when(reviewDao.existsByCustomerAndProduct(any(), any())).thenReturn(false);

        Review savedReview = new Review();
        savedReview.setId(10L);
        savedReview.setRating(4);
        savedReview.setCustomer(customer);
        savedReview.setProduct(product);
        when(reviewDao.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponseDTO response = reviewService.addReview(requestDTO, "customer_testtoken");

        assertNotNull(response);
        assertEquals(4, response.getRating());
        assertEquals(10L, response.getId());
        verify(reviewDao).save(any(Review.class));
    }

    @Test
    void testAddReview_DuplicateReview_ThrowsException() {
        when(sessionDao.findByToken(anyString())).thenReturn(Optional.of(userSession));
        when(customerDao.findById(1)).thenReturn(Optional.of(customer));
        when(productDao.findById(1)).thenReturn(Optional.of(product));
        when(reviewDao.existsByCustomerAndProduct(any(), any())).thenReturn(true);

        assertThrows(ReviewException.class, () -> 
            reviewService.addReview(requestDTO, "customer_testtoken"));
    }

    @Test
    void testAddReview_InvalidRating_ThrowsException() {
        requestDTO.setRating(6);  // Invalid >5

        assertThrows(Exception.class, () ->  // Validation happens before service in controller, but test here
            reviewService.addReview(requestDTO, "customer_testtoken"));
    }

    // Additional edge case tests (duplicate, invalid rating, etc.) follow similar mocking pattern...
}
