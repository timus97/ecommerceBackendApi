package com.masai.models;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned when viewing a single wishlist item.
 * Exposes only the fields relevant to the customer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponseDTO {

    private Integer wishlistItemId;
    private Integer productId;
    private String productName;
    private Double price;
    private String description;
    private String manufacturer;
    private CategoryEnum category;
    private ProductStatus status;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime addedAt;
}
