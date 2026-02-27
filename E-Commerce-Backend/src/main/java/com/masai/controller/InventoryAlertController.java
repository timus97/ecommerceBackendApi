package com.masai.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.masai.dto.InventoryAlertRequestDTO;
import com.masai.dto.InventoryAlertResponseDTO;
import com.masai.dto.InventoryAlertSummaryDTO;
import com.masai.service.InventoryAlertService;

/**
 * REST Controller for Inventory Alert Management
 * Provides endpoints for sellers to configure and manage inventory alerts
 */
@RestController
@RequestMapping("/inventory-alerts")
public class InventoryAlertController {

    @Autowired
    private InventoryAlertService inventoryAlertService;

    /**
     * Create a new inventory alert for a product
     * @param token Seller's session token
     * @param requestDTO Alert configuration
     * @return Created alert with HTTP 201
     */
    @PostMapping
    public ResponseEntity<InventoryAlertResponseDTO> createAlert(
            @RequestHeader("token") String token,
            @Valid @RequestBody InventoryAlertRequestDTO requestDTO) {
        InventoryAlertResponseDTO response = inventoryAlertService.createAlert(token, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update an existing inventory alert
     * @param token Seller's session token
     * @param alertId ID of alert to update
     * @param requestDTO Updated alert configuration
     * @return Updated alert with HTTP 200
     */
    @PutMapping("/{alertId}")
    public ResponseEntity<InventoryAlertResponseDTO> updateAlert(
            @RequestHeader("token") String token,
            @PathVariable Integer alertId,
            @Valid @RequestBody InventoryAlertRequestDTO requestDTO) {
        InventoryAlertResponseDTO response = inventoryAlertService.updateAlert(token, alertId, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Delete an inventory alert
     * @param token Seller's session token
     * @param alertId ID of alert to delete
     * @return Success message with HTTP 200
     */
    @DeleteMapping("/{alertId}")
    public ResponseEntity<String> deleteAlert(
            @RequestHeader("token") String token,
            @PathVariable Integer alertId) {
        String response = inventoryAlertService.deleteAlert(token, alertId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get all alerts for the logged-in seller
     * @param token Seller's session token
     * @return List of all alerts with HTTP 200
     */
    @GetMapping
    public ResponseEntity<List<InventoryAlertResponseDTO>> getAllAlerts(
            @RequestHeader("token") String token) {
        List<InventoryAlertResponseDTO> alerts = inventoryAlertService.getAllAlertsForSeller(token);
        return new ResponseEntity<>(alerts, HttpStatus.OK);
    }

    /**
     * Get all enabled alerts for the logged-in seller
     * @param token Seller's session token
     * @return List of enabled alerts with HTTP 200
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<InventoryAlertResponseDTO>> getEnabledAlerts(
            @RequestHeader("token") String token) {
        List<InventoryAlertResponseDTO> alerts = inventoryAlertService.getEnabledAlertsForSeller(token);
        return new ResponseEntity<>(alerts, HttpStatus.OK);
    }

    /**
     * Get all triggered alerts (where current quantity <= threshold) for the logged-in seller
     * @param token Seller's session token
     * @return List of triggered alerts with HTTP 200
     */
    @GetMapping("/triggered")
    public ResponseEntity<List<InventoryAlertSummaryDTO>> getTriggeredAlerts(
            @RequestHeader("token") String token) {
        List<InventoryAlertSummaryDTO> alerts = inventoryAlertService.getTriggeredAlertsForSeller(token);
        return new ResponseEntity<>(alerts, HttpStatus.OK);
    }

    /**
     * Get a specific alert by ID
     * @param token Seller's session token
     * @param alertId Alert ID
     * @return Alert details with HTTP 200
     */
    @GetMapping("/{alertId}")
    public ResponseEntity<InventoryAlertResponseDTO> getAlertById(
            @RequestHeader("token") String token,
            @PathVariable Integer alertId) {
        InventoryAlertResponseDTO alert = inventoryAlertService.getAlertById(token, alertId);
        return new ResponseEntity<>(alert, HttpStatus.OK);
    }

    /**
     * Enable or disable an alert
     * @param token Seller's session token
     * @param alertId Alert ID
     * @param enabled Enable status (true/false)
     * @return Updated alert details with HTTP 200
     */
    @PutMapping("/{alertId}/status")
    public ResponseEntity<InventoryAlertResponseDTO> toggleAlertStatus(
            @RequestHeader("token") String token,
            @PathVariable Integer alertId,
            @RequestParam Boolean enabled) {
        InventoryAlertResponseDTO response = inventoryAlertService.toggleAlertStatus(token, alertId, enabled);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get alert for a specific product
     * @param token Seller's session token
     * @param productId Product ID
     * @return Alert details with HTTP 200
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryAlertResponseDTO> getAlertByProductId(
            @RequestHeader("token") String token,
            @PathVariable Integer productId) {
        InventoryAlertResponseDTO response = inventoryAlertService.getAlertByProductId(token, productId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}