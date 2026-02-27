package com.masai.service;

import java.util.List;

import com.masai.dto.InventoryAlertRequestDTO;
import com.masai.dto.InventoryAlertResponseDTO;
import com.masai.dto.InventoryAlertSummaryDTO;

/**
 * Service interface for managing inventory alerts
 */
public interface InventoryAlertService {

    /**
     * Create a new inventory alert for a product
     * @param token Seller's session token
     * @param requestDTO Alert configuration request
     * @return Created alert details
     */
    InventoryAlertResponseDTO createAlert(String token, InventoryAlertRequestDTO requestDTO);

    /**
     * Update an existing inventory alert
     * @param token Seller's session token
     * @param alertId ID of the alert to update
     * @param requestDTO Updated alert configuration
     * @return Updated alert details
     */
    InventoryAlertResponseDTO updateAlert(String token, Integer alertId, InventoryAlertRequestDTO requestDTO);

    /**
     * Delete an inventory alert
     * @param token Seller's session token
     * @param alertId ID of the alert to delete
     * @return Success message
     */
    String deleteAlert(String token, Integer alertId);

    /**
     * Get all alerts for the logged-in seller
     * @param token Seller's session token
     * @return List of all alerts
     */
    List<InventoryAlertResponseDTO> getAllAlertsForSeller(String token);

    /**
     * Get all enabled alerts for the logged-in seller
     * @param token Seller's session token
     * @return List of enabled alerts
     */
    List<InventoryAlertResponseDTO> getEnabledAlertsForSeller(String token);

    /**
     * Get all triggered alerts (where current quantity <= threshold) for the logged-in seller
     * @param token Seller's session token
     * @return List of triggered alerts
     */
    List<InventoryAlertSummaryDTO> getTriggeredAlertsForSeller(String token);

    /**
     * Get a specific alert by ID
     * @param token Seller's session token
     * @param alertId Alert ID
     * @return Alert details
     */
    InventoryAlertResponseDTO getAlertById(String token, Integer alertId);

    /**
     * Enable or disable an alert
     * @param token Seller's session token
     * @param alertId Alert ID
     * @param enabled Enable status
     * @return Updated alert details
     */
    InventoryAlertResponseDTO toggleAlertStatus(String token, Integer alertId, Boolean enabled);

    /**
     * Check and return all triggered alerts across all sellers (for admin/scheduler use)
     * @return List of all triggered alerts
     */
    List<InventoryAlertSummaryDTO> getAllTriggeredAlerts();

    /**
     * Get alert for a specific product
     * @param token Seller's session token
     * @param productId Product ID
     * @return Alert details if exists
     */
    InventoryAlertResponseDTO getAlertByProductId(String token, Integer productId);
}