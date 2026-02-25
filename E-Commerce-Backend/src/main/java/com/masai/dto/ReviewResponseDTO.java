package com.masai.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for outgoing review responses (avoids direct entity exposure in controllers).
 * Includes all relevant fields, timestamps, and moderation status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {

    private Long id;
    private Integer rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isApproved;
    private Integer helpfulCount;
    private Integer productId;  // Reference only (no full product)
    private Integer customerId;  // Reviewer reference (no full customer)
}
