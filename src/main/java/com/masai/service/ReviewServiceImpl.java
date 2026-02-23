package com.masai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.masai.exception.LoginException;
import com.masai.exception.ReviewException;
import com.masai.models.Customer;
import com.masai.models.Product;
import com.masai.models.Review;
import com.masai.models.ReviewRequestDTO;
import com.masai.models.ReviewResponseDTO;
import com.masai.models.ReviewSummaryDTO;
import com.masai.models.UserSession;
import com.masai.repository.CustomerDao;
import com.masai.repository.ProductDao;
import com.masai.repository.ReviewDao;
import com.masai.repository.SessionDao;

/**
 * Full ReviewServiceImpl with all operations, validations, and exception handling.
 * Uses DTOs to avoid direct entity exposure.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewDao reviewDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private LoginLogoutService loginService;

    @Override
    public ReviewResponseDTO addReview(ReviewRequestDTO reviewRequest, String token) throws ReviewException, LoginException {
        // 1. Token validation (customer only)
        if (token == null || !token.contains("customer")) {
            throw new LoginException("Invalid session token for customer");
        }
        loginService.checkTokenStatus(token);

        // 2. Get logged-in customer
        UserSession userSession = sessionDao.findByToken(token)
            .orElseThrow(() -> new LoginException("Invalid or expired session token"));
        Customer customer = customerDao.findById(userSession.getUserId())
            .orElseThrow(() -> new ReviewException("Customer not found"));

        // 3. Validate product exists
        Product product = productDao.findById(reviewRequest.getProductId())
            .orElseThrow(() -> new ReviewException("Product not found with ID: " + reviewRequest.getProductId()));

        // 4. Duplicate check (service + DB constraint)
        if (reviewDao.existsByCustomerAndProduct(customer, product)) {
            throw new ReviewException("You have already submitted a review for this product");
        }

        // 5. Create entity from DTO
        Review review = new Review();
        review.setRating(reviewRequest.getRating());
        review.setTitle(reviewRequest.getTitle());
        review.setComment(reviewRequest.getComment());
        review.setProduct(product);
        review.setCustomer(customer);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        review.setIsDeleted(false);
        review.setIsApproved(false);  // Requires moderation
        review.setHelpfulCount(0);

        // 6. Save and map to response DTO
        Review saved = reviewDao.save(review);
        
        // Auto-recalculate Product rating stats
        recalculateAndUpdateProduct(saved.getProduct());
        
        return mapToResponseDTO(saved);
    }

    @Override
    public ReviewResponseDTO updateReview(Long reviewId, ReviewRequestDTO reviewRequest, String token) 
            throws ReviewException, LoginException {
        // Token validation
        if (token == null || !token.contains("customer")) {
            throw new LoginException("Invalid session token for customer");
        }
        loginService.checkTokenStatus(token);

        // Find review
        Review review = reviewDao.findById(reviewId)
            .orElseThrow(() -> new ReviewException("Review not found with ID: " + reviewId));

        // Ownership check (only owner can update)
        UserSession userSession = sessionDao.findByToken(token)
            .orElseThrow(() -> new LoginException("Invalid session"));
        if (!review.getCustomer().getCustomerId().equals(userSession.getUserId())) {
            throw new ReviewException("You can only update your own reviews");
        }

        // Update fields
        review.setRating(reviewRequest.getRating());
        review.setTitle(reviewRequest.getTitle());
        review.setComment(reviewRequest.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        Review updated = reviewDao.save(review);
        
        // Auto-recalculate Product rating stats
        recalculateAndUpdateProduct(updated.getProduct());
        
        return mapToResponseDTO(updated);
    }

    @Override
    public ReviewResponseDTO deleteReview(Long reviewId, String token) throws ReviewException, LoginException {
        // Token validation
        if (token == null || !token.contains("customer")) {
            throw new LoginException("Invalid session token for customer");
        }
        loginService.checkTokenStatus(token);

        // Find review
        Review review = reviewDao.findById(reviewId)
            .orElseThrow(() -> new ReviewException("Review not found with ID: " + reviewId));

        // Ownership check
        UserSession userSession = sessionDao.findByToken(token)
            .orElseThrow(() -> new LoginException("Invalid session"));
        if (!review.getCustomer().getCustomerId().equals(userSession.getUserId())) {
            throw new ReviewException("You can only delete your own reviews");
        }

        // Soft delete
        review.setIsDeleted(true);
        review.setUpdatedAt(LocalDateTime.now());
        Review deleted = reviewDao.save(review);
        
        // Auto-recalculate Product rating stats
        recalculateAndUpdateProduct(deleted.getProduct());
        
        return mapToResponseDTO(deleted);
    }

    @Override
    public Page<ReviewResponseDTO> getProductReviews(Integer productId, Pageable pageable) {
        // Get approved + non-deleted reviews (paginated)
        Page<Review> reviewsPage = reviewDao.findByProductIdAndIsApprovedTrue(productId, pageable);
        return reviewsPage.map(this::mapToResponseDTO);
    }

    @Override
    public ReviewResponseDTO approveReview(Long reviewId, String token) throws ReviewException, LoginException {
        // Token validation (assume seller/admin; simple check for now)
        if (token == null) {
            throw new LoginException("Invalid session token");
        }
        loginService.checkTokenStatus(token);

        // Find review
        Review review = reviewDao.findById(reviewId)
            .orElseThrow(() -> new ReviewException("Review not found with ID: " + reviewId));

        // Approve
        review.setIsApproved(true);
        review.setUpdatedAt(LocalDateTime.now());
        Review approved = reviewDao.save(review);
        
        // Auto-recalculate Product rating stats (approve affects avg)
        recalculateAndUpdateProduct(approved.getProduct());
        
        return mapToResponseDTO(approved);
    }

    @Override
    public ReviewSummaryDTO calculateProductRating(Integer productId) {
        // Use custom query for average + count
        Double avg = reviewDao.calculateAverageRating(productId);
        long count = reviewDao.countByProductId(productId);
        return new ReviewSummaryDTO(avg != null ? avg : 0.0, count, productId);
    }

    // Helper mapper: Entity -> ResponseDTO
    private ReviewResponseDTO mapToResponseDTO(Review review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setTitle(review.getTitle());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        dto.setIsApproved(review.getIsApproved());
        dto.setHelpfulCount(review.getHelpfulCount());
        dto.setProductId(review.getProduct() != null ? review.getProduct().getProductId() : null);
        dto.setCustomerId(review.getCustomer() != null ? review.getCustomer().getCustomerId() : null);
        return dto;
    }

    // Auto-recalculate and update Product's rating stats after review changes
    private void recalculateAndUpdateProduct(Product product) {
        if (product == null) return;
        ReviewSummaryDTO summary = calculateProductRating(product.getProductId());
        product.setAverageRating(summary.getAverageRating());
        product.setReviewCount(summary.getTotalReviews());
        productDao.save(product);
    }
}
