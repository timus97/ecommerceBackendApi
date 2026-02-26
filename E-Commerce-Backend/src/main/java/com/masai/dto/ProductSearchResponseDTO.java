package com.masai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchResponseDTO {

	private Integer productId;
	private String productName;
	private Double price;
	private String description;
	private String manufacturer;
	private Integer quantity;
	private String category;
	private String status;
	private Double averageRating;
	private Long reviewCount;
	private String sellerName;

}
