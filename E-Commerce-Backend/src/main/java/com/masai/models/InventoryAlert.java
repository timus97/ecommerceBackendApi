package com.masai.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity representing an inventory alert for a product.
 * Alerts are triggered when product quantity falls below the configured threshold.
 */
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class InventoryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer alertId;

    @NotNull(message = "Product is required")
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Seller is required")
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnore
    private Seller seller;

    @NotNull(message = "Threshold quantity is required")
    @Min(value = 0, message = "Threshold must be at least 0")
    @Column(nullable = false)
    private Integer thresholdQuantity;

    @NotNull(message = "Alert enabled status is required")
    @Column(nullable = false)
    private Boolean alertEnabled = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastAlertSentAt;

    @Column
    private Integer alertCount = 0;

    /**
     * Lifecycle callback to set timestamps before persisting
     */
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    /**
     * Lifecycle callback to update timestamp before updating
     */
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment alert count and update last alert sent time
     */
    public void recordAlertSent() {
        this.lastAlertSentAt = LocalDateTime.now();
        this.alertCount = (this.alertCount == null ? 0 : this.alertCount) + 1;
    }
}