package com.masai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for review summary (average rating + total count) - useful for product pages.
 * Avoids exposing full review lists/entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDTO {

    private Double averageRating;
    private Long totalReviews;
    private Integer productId;  // Associated product
}
