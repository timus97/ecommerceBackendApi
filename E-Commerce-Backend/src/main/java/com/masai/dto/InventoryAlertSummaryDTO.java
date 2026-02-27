package com.masai.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for summarizing triggered inventory alerts for sellers
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAlertSummaryDTO {

    private Integer alertId;
    private Integer productId;
    private String productName;
    private Integer thresholdQuantity;
    private Integer currentQuantity;
    private Integer quantityToRestock;
    private LocalDateTime lastAlertSentAt;
}