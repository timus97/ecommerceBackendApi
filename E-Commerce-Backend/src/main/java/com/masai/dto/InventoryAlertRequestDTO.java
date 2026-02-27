package com.masai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating or updating an inventory alert
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertRequestDTO {

    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotNull(message = "Threshold quantity is required")
    @Min(value = 0, message = "Threshold must be at least 0")
    private Integer thresholdQuantity;

    private Boolean alertEnabled = true;
}