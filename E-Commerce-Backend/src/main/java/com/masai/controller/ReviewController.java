package com.masai.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.masai.dto.ReviewRequestDTO;
import com.masai.dto.ReviewResponseDTO;
import com.masai.dto.ReviewSummaryDTO;
import com.masai.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ReviewController with endpoints for product reviews.
 * Supports pagination/sorting and token-based auth.
 * Swagger documented with example responses.
 */
@RestController
@Tag(name = "Reviews", description = "Product review management APIs")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // POST /products/{id}/reviews - Add review to product
    @Operation(summary = "Add a new review to a product", description = "Requires valid customer token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid rating or duplicate review"),
        @ApiResponse(responseCode = "403", description = "Invalid token")
    })
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ReviewResponseDTO> addReviewHandler(
            @Parameter(description = "Product ID") @PathVariable("productId") Integer productId,
            @Valid @RequestBody ReviewRequestDTO reviewRequest,
            @Parameter(description = "Auth token") @RequestHeader("token") String token) {
        
        // Set productId from path if not in body
        reviewRequest.setProductId(productId);
        ReviewResponseDTO response = reviewService.addReview(reviewRequest, token);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // PUT /reviews/{id} - Update review
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReviewHandler(
            @PathVariable("reviewId") Long reviewId,
            @Valid @RequestBody ReviewRequestDTO reviewRequest,
            @RequestHeader("token") String token) {
        
        ReviewResponseDTO response = reviewService.updateReview(reviewId, reviewRequest, token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // DELETE /reviews/{id} - Soft delete review
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> deleteReviewHandler(
            @PathVariable("reviewId") Long reviewId,
            @RequestHeader("token") String token) {
        
        ReviewResponseDTO response = reviewService.deleteReview(reviewId, token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // GET /products/{id}/reviews - Paginated reviews for product (with sorting)
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponseDTO>> getProductReviewsHandler(
            @PathVariable("productId") Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction dir = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        Page<ReviewResponseDTO> reviews = reviewService.getProductReviews(productId, pageable);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }

    // GET /products/{id}/reviews/summary - Rating summary
    @GetMapping("/products/{productId}/reviews/summary")
    public ResponseEntity<ReviewSummaryDTO> getProductReviewSummaryHandler(
            @PathVariable("productId") Integer productId) {
        
        ReviewSummaryDTO summary = reviewService.calculateProductRating(productId);
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }
}
