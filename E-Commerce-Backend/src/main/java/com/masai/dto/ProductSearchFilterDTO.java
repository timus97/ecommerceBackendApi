package com.masai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchFilterDTO {

	// Search keyword - searches in product name and description
	private String keyword;

	// Filter criteria
	private String category; // Category enum value as string
	private String status; // ProductStatus enum value as string
	private Double minPrice;
	private Double maxPrice;
	private Double minRating;
	private String manufacturer;
	private Integer sellerId;

	// Pagination
	private Integer page = 0;
	private Integer size = 10;

}
