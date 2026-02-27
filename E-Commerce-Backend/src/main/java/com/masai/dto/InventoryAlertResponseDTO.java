package com.masai.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for inventory alert response
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAlertResponseDTO {

    private Integer alertId;
    private Integer productId;
    private String productName;
    private Integer sellerId;
    private String sellerName;
    private Integer thresholdQuantity;
    private Integer currentQuantity;
    private Boolean alertEnabled;
    private Boolean alertTriggered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAlertSentAt;
    private Integer alertCount;
}