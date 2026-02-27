package com.masai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.masai.dto.InventoryAlertRequestDTO;
import com.masai.dto.InventoryAlertResponseDTO;
import com.masai.dto.InventoryAlertSummaryDTO;
import com.masai.service.InventoryAlertService;

@DisplayName("InventoryAlertController Tests")
@ExtendWith(MockitoExtension.class)
class InventoryAlertControllerTest {

    @Mock
    private InventoryAlertService inventoryAlertService;

    @InjectMocks
    private InventoryAlertController inventoryAlertController;

    private InventoryAlertResponseDTO alertResponseDTO;
    private InventoryAlertRequestDTO alertRequestDTO;
    private InventoryAlertSummaryDTO alertSummaryDTO;

    @BeforeEach
    void setUp() {
        alertResponseDTO = new InventoryAlertResponseDTO();
        alertResponseDTO.setAlertId(1);
        alertResponseDTO.setProductId(1);
        alertResponseDTO.setThresholdQuantity(10);

        alertRequestDTO = new InventoryAlertRequestDTO();
        alertRequestDTO.setProductId(1);
        alertRequestDTO.setThresholdQuantity(10);

        alertSummaryDTO = new InventoryAlertSummaryDTO();
        alertSummaryDTO.setAlertId(1);
        alertSummaryDTO.setProductId(1);
    }

    @Test
    @DisplayName("Should create alert")
    void testCreateAlert() {
        when(inventoryAlertService.createAlert(anyString(), any(InventoryAlertRequestDTO.class)))
            .thenReturn(alertResponseDTO);

        ResponseEntity<InventoryAlertResponseDTO> response = 
            inventoryAlertController.createAlert("token", alertRequestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getAlertId());
    }

    @Test
    @DisplayName("Should update alert")
    void testUpdateAlert() {
        when(inventoryAlertService.updateAlert(anyString(), anyInt(), any(InventoryAlertRequestDTO.class)))
            .thenReturn(alertResponseDTO);

        ResponseEntity<InventoryAlertResponseDTO> response = 
            inventoryAlertController.updateAlert("token", 1, alertRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should delete alert")
    void testDeleteAlert() {
        when(inventoryAlertService.deleteAlert(anyString(), anyInt()))
            .thenReturn("Alert deleted");

        ResponseEntity<String> response = inventoryAlertController.deleteAlert("token", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Alert deleted", response.getBody());
    }

    @Test
    @DisplayName("Should get all alerts")
    void testGetAllAlerts() {
        when(inventoryAlertService.getAllAlertsForSeller(anyString()))
            .thenReturn(Arrays.asList(alertResponseDTO));

        ResponseEntity<List<InventoryAlertResponseDTO>> response = 
            inventoryAlertController.getAllAlerts("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Should get enabled alerts")
    void testGetEnabledAlerts() {
        when(inventoryAlertService.getEnabledAlertsForSeller(anyString()))
            .thenReturn(Arrays.asList(alertResponseDTO));

        ResponseEntity<List<InventoryAlertResponseDTO>> response = 
            inventoryAlertController.getEnabledAlerts("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get triggered alerts")
    void testGetTriggeredAlerts() {
        when(inventoryAlertService.getTriggeredAlertsForSeller(anyString()))
            .thenReturn(Arrays.asList(alertSummaryDTO));

        ResponseEntity<List<InventoryAlertSummaryDTO>> response = 
            inventoryAlertController.getTriggeredAlerts("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get alert by ID")
    void testGetAlertById() {
        when(inventoryAlertService.getAlertById(anyString(), anyInt()))
            .thenReturn(alertResponseDTO);

        ResponseEntity<InventoryAlertResponseDTO> response = 
            inventoryAlertController.getAlertById("token", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should toggle alert status")
    void testToggleAlertStatus() {
        when(inventoryAlertService.toggleAlertStatus(anyString(), anyInt(), anyBoolean()))
            .thenReturn(alertResponseDTO);

        ResponseEntity<InventoryAlertResponseDTO> response = 
            inventoryAlertController.toggleAlertStatus("token", 1, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get alert by product ID")
    void testGetAlertByProductId() {
        when(inventoryAlertService.getAlertByProductId(anyString(), anyInt()))
            .thenReturn(alertResponseDTO);

        ResponseEntity<InventoryAlertResponseDTO> response = 
            inventoryAlertController.getAlertByProductId("token", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
