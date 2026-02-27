package com.masai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.masai.models.InventoryAlert;

/**
 * Repository for InventoryAlert entity operations
 */
@Repository
public interface InventoryAlertDao extends JpaRepository<InventoryAlert, Integer> {

    /**
     * Find all alerts for a specific seller
     */
    List<InventoryAlert> findBySeller_SellerId(Integer sellerId);

    /**
     * Find all enabled alerts for a specific seller
     */
    @Query("SELECT ia FROM InventoryAlert ia WHERE ia.seller.sellerId = :sellerId AND ia.alertEnabled = true")
    List<InventoryAlert> findEnabledAlertsBySellerId(@Param("sellerId") Integer sellerId);

    /**
     * Find alert by product id
     */
    Optional<InventoryAlert> findByProduct_ProductId(Integer productId);

    /**
     * Check if an alert exists for a product
     */
    boolean existsByProduct_ProductId(Integer productId);

    /**
     * Find all alerts where product quantity is below threshold and alert is enabled
     */
    @Query("SELECT ia FROM InventoryAlert ia WHERE ia.alertEnabled = true AND ia.product.quantity <= ia.thresholdQuantity")
    List<InventoryAlert> findTriggeredAlerts();

    /**
     * Find all triggered alerts for a specific seller
     */
    @Query("SELECT ia FROM InventoryAlert ia WHERE ia.seller.sellerId = :sellerId AND ia.alertEnabled = true AND ia.product.quantity <= ia.thresholdQuantity")
    List<InventoryAlert> findTriggeredAlertsBySellerId(@Param("sellerId") Integer sellerId);

    /**
     * Delete alert by product id
     */
    void deleteByProduct_ProductId(Integer productId);
}
