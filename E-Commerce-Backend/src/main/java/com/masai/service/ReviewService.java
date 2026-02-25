package com.masai.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.masai.exception.LoginException;
import com.masai.exception.ReviewException;
import com.masai.dto.ReviewRequestDTO;
import com.masai.dto.ReviewResponseDTO;
import com.masai.dto.ReviewSummaryDTO;

/**
 * Service interface for Review operations.
 * Includes CRUD, moderation, pagination, and rating calculations with validations.
 */
public interface ReviewService {

    // Add a new review (with duplicate check and token validation)
    ReviewResponseDTO addReview(ReviewRequestDTO reviewRequest, String token) throws ReviewException, LoginException;

    // Update an existing review (owner only)
    ReviewResponseDTO updateReview(Long reviewId, ReviewRequestDTO reviewRequest, String token) throws ReviewException, LoginException;

    // Soft delete a review (owner only)
    ReviewResponseDTO deleteReview(Long reviewId, String token) throws ReviewException, LoginException;

    // Get paginated reviews for a product (approved + non-deleted)
    Page<ReviewResponseDTO> getProductReviews(Integer productId, Pageable pageable);

    // Approve a review (admin/seller moderation)
    ReviewResponseDTO approveReview(Long reviewId, String token) throws ReviewException, LoginException;

    // Calculate summary (avg rating + count) for a product
    ReviewSummaryDTO calculateProductRating(Integer productId);
}
