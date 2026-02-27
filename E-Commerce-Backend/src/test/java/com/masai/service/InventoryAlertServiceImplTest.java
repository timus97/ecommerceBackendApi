package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.dto.InventoryAlertRequestDTO;
import com.masai.dto.InventoryAlertResponseDTO;
import com.masai.dto.InventoryAlertSummaryDTO;
import com.masai.exception.InventoryAlertException;
import com.masai.exception.ProductNotFoundException;
import com.masai.models.InventoryAlert;
import com.masai.models.Product;
import com.masai.models.Seller;
import com.masai.models.UserSession;
import com.masai.repository.InventoryAlertDao;
import com.masai.repository.ProductRepository;
import com.masai.util.TokenValidationUtil;

/**
 * Unit tests for InventoryAlertServiceImpl
 * Covers all service methods with various scenarios including success and failure cases
 */
@DisplayName("InventoryAlertServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class InventoryAlertServiceImplTest {

    @Mock
    private InventoryAlertDao inventoryAlertDao;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerService sellerService;

    @Mock
    private TokenValidationUtil tokenValidationUtil;

    @InjectMocks
    private InventoryAlertServiceImpl inventoryAlertService;

    private static final String VALID_SELLER_TOKEN = "seller_valid_token";
    private static final String INVALID_TOKEN = "invalid_token";
    private static final Integer SELLER_ID = 1;
    private static final Integer PRODUCT_ID = 100;
    private static final Integer ALERT_ID = 50;

    private UserSession sellerSession;
    private Seller seller;
    private Product product;
    private InventoryAlert inventoryAlert;
    private InventoryAlertRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        // Setup seller session
        sellerSession = new UserSession();
        sellerSession.setUserId(SELLER_ID);
        sellerSession.setToken(VALID_SELLER_TOKEN);
        sellerSession.setUserType("seller");

        // Setup seller
        seller = new Seller();
        seller.setSellerId(SELLER_ID);
        seller.setFirstName("John");
        seller.setLastName("Doe");
        seller.setMobile("9876543210");

        // Setup product
        product = new Product();
        product.setProductId(PRODUCT_ID);
        product.setProductName("Test Product");
        product.setQuantity(10);
        product.setPrice(99.99);
        product.setSeller(seller);

        // Setup inventory alert
        inventoryAlert = new InventoryAlert();
        inventoryAlert.setAlertId(ALERT_ID);
        inventoryAlert.setProduct(product);
        inventoryAlert.setSeller(seller);
        inventoryAlert.setThresholdQuantity(15);
        inventoryAlert.setAlertEnabled(true);
        inventoryAlert.setCreatedAt(LocalDateTime.now());
        inventoryAlert.setUpdatedAt(LocalDateTime.now());
        inventoryAlert.setAlertCount(0);

        // Setup request DTO
        requestDTO = new InventoryAlertRequestDTO();
        requestDTO.setProductId(PRODUCT_ID);
        requestDTO.setThresholdQuantity(15);
        requestDTO.setAlertEnabled(true);
    }

    // ==================== CREATE ALERT TESTS ====================

    @Test
    @DisplayName("Should create alert successfully when valid data provided")
    void testCreateAlert_Success() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryAlertDao.existsByProduct_ProductId(PRODUCT_ID)).thenReturn(false);
        when(inventoryAlertDao.save(any(InventoryAlert.class))).thenReturn(inventoryAlert);

        InventoryAlertResponseDTO response = inventoryAlertService.createAlert(VALID_SELLER_TOKEN, requestDTO);

        assertNotNull(response);
        assertEquals(ALERT_ID, response.getAlertId());
        assertEquals(PRODUCT_ID, response.getProductId());
        assertEquals("Test Product", response.getProductName());
        assertEquals(15, response.getThresholdQuantity());
        assertEquals(10, response.getCurrentQuantity());
        assertTrue(response.getAlertEnabled());
        assertTrue(response.getAlertTriggered()); // quantity 10 <= threshold 15

        verify(inventoryAlertDao).save(any(InventoryAlert.class));
    }

    @Test
    @DisplayName("Should throw exception when creating alert for non-existent product")
    void testCreateAlert_ProductNotFound() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
            inventoryAlertService.createAlert(VALID_SELLER_TOKEN, requestDTO));
    }

    @Test
    @DisplayName("Should throw exception when creating alert for product not owned by seller")
    void testCreateAlert_ProductNotOwnedBySeller() {
        Seller otherSeller = new Seller();
        otherSeller.setSellerId(999);
        product.setSeller(otherSeller);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThrows(InventoryAlertException.class, () ->
            inventoryAlertService.createAlert(VALID_SELLER_TOKEN, requestDTO));
    }

    @Test
    @DisplayName("Should throw exception when alert already exists for product")
    void testCreateAlert_AlreadyExists() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryAlertDao.existsByProduct_ProductId(PRODUCT_ID)).thenReturn(true);

        assertThrows(InventoryAlertException.class, () ->
            inventoryAlertService.createAlert(VALID_SELLER_TOKEN, requestDTO));
    }

    @Test
    @DisplayName("Should throw exception when invalid token provided")
    void testCreateAlert_InvalidToken() {
        when(tokenValidationUtil.validateSellerToken(INVALID_TOKEN))
            .thenThrow(new com.masai.exception.LoginException("Invalid token"));

        assertThrows(com.masai.exception.LoginException.class, () ->
            inventoryAlertService.createAlert(INVALID_TOKEN, requestDTO));
    }

    // ==================== UPDATE ALERT TESTS ====================

    @Test
    @DisplayName("Should update alert successfully")
    void testUpdateAlert_Success() {
        requestDTO.setThresholdQuantity(20);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));
        when(inventoryAlertDao.save(any(InventoryAlert.class))).thenReturn(inventoryAlert);

        InventoryAlertResponseDTO response = inventoryAlertService.updateAlert(VALID_SELLER_TOKEN, ALERT_ID, requestDTO);

        assertNotNull(response);
        verify(inventoryAlertDao).save(any(InventoryAlert.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent alert")
    void testUpdateAlert_NotFound() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.empty());

        assertThrows(InventoryAlertException.class, () ->
            inventoryAlertService.updateAlert(VALID_SELLER_TOKEN, ALERT_ID, requestDTO));
    }

    @Test
    @DisplayName("Should throw exception when updating alert not owned by seller")
    void testUpdateAlert_NotOwnedBySeller() {
        Seller otherSeller = new Seller();
        otherSeller.setSellerId(999);
        inventoryAlert.setSeller(otherSeller);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));

        assertThrows(InventoryAlertException.class, () ->
            inventoryAlertService.updateAlert(VALID_SELLER_TOKEN, ALERT_ID, requestDTO));
    }

    // ==================== DELETE ALERT TESTS ====================

    @Test
    @DisplayName("Should delete alert successfully")
    void testDeleteAlert_Success() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));
        doNothing().when(inventoryAlertDao).delete(inventoryAlert);

        String result = inventoryAlertService.deleteAlert(VALID_SELLER_TOKEN, ALERT_ID);

        assertEquals("Alert deleted successfully", result);
        verify(inventoryAlertDao).delete(inventoryAlert);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent alert")
    void testDeleteAlert_NotFound() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.empty());

        assertThrows(InventoryAlertException.class, () ->
            inventoryAlertService.deleteAlert(VALID_SELLER_TOKEN, ALERT_ID));
    }

    // ==================== GET ALERTS TESTS ====================

    @Test
    @DisplayName("Should get all alerts for seller")
    void testGetAllAlertsForSeller_Success() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(inventoryAlertDao.findBySeller_SellerId(SELLER_ID)).thenReturn(Arrays.asList(inventoryAlert));

        List<InventoryAlertResponseDTO> alerts = inventoryAlertService.getAllAlertsForSeller(VALID_SELLER_TOKEN);

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals(ALERT_ID, alerts.get(0).getAlertId());
    }

    @Test
    @DisplayName("Should return empty list when seller has no alerts")
    void testGetAllAlertsForSeller_EmptyList() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(inventoryAlertDao.findBySeller_SellerId(SELLER_ID)).thenReturn(Collections.emptyList());

        List<InventoryAlertResponseDTO> alerts = inventoryAlertService.getAllAlertsForSeller(VALID_SELLER_TOKEN);

        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    @Test
    @DisplayName("Should get enabled alerts for seller")
    void testGetEnabledAlertsForSeller_Success() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(inventoryAlertDao.findEnabledAlertsBySellerId(SELLER_ID)).thenReturn(Arrays.asList(inventoryAlert));

        List<InventoryAlertResponseDTO> alerts = inventoryAlertService.getEnabledAlertsForSeller(VALID_SELLER_TOKEN);

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).getAlertEnabled());
    }

    @Test
    @DisplayName("Should get triggered alerts for seller")
    void testGetTriggeredAlertsForSeller_Success() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(inventoryAlertDao.findTriggeredAlertsBySellerId(SELLER_ID)).thenReturn(Arrays.asList(inventoryAlert));

        List<InventoryAlertSummaryDTO> alerts = inventoryAlertService.getTriggeredAlertsForSeller(VALID_SELLER_TOKEN);

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals(ALERT_ID, alerts.get(0).getAlertId());
        assertEquals(6, alerts.get(0).getQuantityToRestock()); // threshold 15 - current 10 + 1 = 6
    }

    @Test
    @DisplayName("Should get alert by ID")
    void testGetAlertById_Success() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));

        InventoryAlertResponseDTO response = inventoryAlertService.getAlertById(VALID_SELLER_TOKEN, ALERT_ID);

        assertNotNull(response);
        assertEquals(ALERT_ID, response.getAlertId());
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent alert")
    void testGetAlertById_NotFound() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.empty());

        assertThrows(InventoryAlertException.class, () ->
            inventoryAlertService.getAlertById(VALID_SELLER_TOKEN, ALERT_ID));
    }

    // ==================== TOGGLE ALERT STATUS TESTS ====================

    @Test
    @DisplayName("Should disable alert successfully")
    void testToggleAlertStatus_Disable() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));
        when(inventoryAlertDao.save(any(InventoryAlert.class))).thenReturn(inventoryAlert);

        InventoryAlertResponseDTO response = inventoryAlertService.toggleAlertStatus(VALID_SELLER_TOKEN, ALERT_ID, false);

        assertNotNull(response);
        assertFalse(response.getAlertEnabled());
        assertFalse(response.getAlertTriggered()); // Should not be triggered when disabled
    }

    @Test
    @DisplayName("Should enable alert successfully")
    void testToggleAlertStatus_Enable() {
        inventoryAlert.setAlertEnabled(false);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));
        when(inventoryAlertDao.save(any(InventoryAlert.class))).thenReturn(inventoryAlert);

        InventoryAlertResponseDTO response = inventoryAlertService.toggleAlertStatus(VALID_SELLER_TOKEN, ALERT_ID, true);

        assertNotNull(response);
        assertTrue(response.getAlertEnabled());
    }

    // ==================== GET ALL TRIGGERED ALERTS TESTS ====================

    @Test
    @DisplayName("Should get all triggered alerts across all sellers")
    void testGetAllTriggeredAlerts_Success() {
        when(inventoryAlertDao.findTriggeredAlerts()).thenReturn(Arrays.asList(inventoryAlert));

        List<InventoryAlertSummaryDTO> alerts = inventoryAlertService.getAllTriggeredAlerts();

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
    }

    @Test
    @DisplayName("Should return empty list when no triggered alerts exist")
    void testGetAllTriggeredAlerts_Empty() {
        when(inventoryAlertDao.findTriggeredAlerts()).thenReturn(Collections.emptyList());

        List<InventoryAlertSummaryDTO> alerts = inventoryAlertService.getAllTriggeredAlerts();

        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    // ==================== GET ALERT BY PRODUCT ID TESTS ====================

    @Test
    @DisplayName("Should get alert by product ID")
    void testGetAlertByProductId_Success() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryAlertDao.findByProduct_ProductId(PRODUCT_ID)).thenReturn(Optional.of(inventoryAlert));

        InventoryAlertResponseDTO response = inventoryAlertService.getAlertByProductId(VALID_SELLER_TOKEN, PRODUCT_ID);

        assertNotNull(response);
        assertEquals(ALERT_ID, response.getAlertId());
        assertEquals(PRODUCT_ID, response.getProductId());
    }

    @Test
    @DisplayName("Should throw exception when no alert exists for product")
    void testGetAlertByProductId_NotFound() {
        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryAlertDao.findByProduct_ProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(InventoryAlertException.class, () ->
            inventoryAlertService.getAlertByProductId(VALID_SELLER_TOKEN, PRODUCT_ID));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("Should handle alert not triggered when quantity above threshold")
    void testAlertNotTriggered_WhenQuantityAboveThreshold() {
        product.setQuantity(20); // Above threshold of 15
        inventoryAlert.setAlertEnabled(true);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));

        InventoryAlertResponseDTO response = inventoryAlertService.getAlertById(VALID_SELLER_TOKEN, ALERT_ID);

        assertNotNull(response);
        assertFalse(response.getAlertTriggered());
    }

    @Test
    @DisplayName("Should handle zero threshold quantity")
    void testCreateAlert_ZeroThreshold() {
        requestDTO.setThresholdQuantity(0);
        inventoryAlert.setThresholdQuantity(0);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryAlertDao.existsByProduct_ProductId(PRODUCT_ID)).thenReturn(false);
        when(inventoryAlertDao.save(any(InventoryAlert.class))).thenReturn(inventoryAlert);

        InventoryAlertResponseDTO response = inventoryAlertService.createAlert(VALID_SELLER_TOKEN, requestDTO);

        assertNotNull(response);
        assertEquals(0, response.getThresholdQuantity());
    }

    @Test
    @DisplayName("Should calculate correct quantity to restock")
    void testQuantityToRestockCalculation() {
        product.setQuantity(5);
        inventoryAlert.setThresholdQuantity(20);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(inventoryAlertDao.findTriggeredAlertsBySellerId(SELLER_ID)).thenReturn(Arrays.asList(inventoryAlert));

        List<InventoryAlertSummaryDTO> alerts = inventoryAlertService.getTriggeredAlertsForSeller(VALID_SELLER_TOKEN);

        assertEquals(1, alerts.size());
        assertEquals(16, alerts.get(0).getQuantityToRestock()); // 20 - 5 + 1 = 16
    }

    @Test
    @DisplayName("Should handle null alert count gracefully")
    void testNullAlertCount() {
        inventoryAlert.setAlertCount(null);

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(sellerService.getSellerById(SELLER_ID)).thenReturn(seller);
        when(inventoryAlertDao.findById(ALERT_ID)).thenReturn(Optional.of(inventoryAlert));

        InventoryAlertResponseDTO response = inventoryAlertService.getAlertById(VALID_SELLER_TOKEN, ALERT_ID);

        assertNotNull(response);
        assertNull(response.getAlertCount());
    }

    @Test
    @DisplayName("Should handle multiple alerts for seller")
    void testGetMultipleAlertsForSeller() {
        Product product2 = new Product();
        product2.setProductId(101);
        product2.setProductName("Second Product");
        product2.setQuantity(5);
        product2.setSeller(seller);

        InventoryAlert alert2 = new InventoryAlert();
        alert2.setAlertId(51);
        alert2.setProduct(product2);
        alert2.setSeller(seller);
        alert2.setThresholdQuantity(10);
        alert2.setAlertEnabled(true);
        alert2.setCreatedAt(LocalDateTime.now());
        alert2.setUpdatedAt(LocalDateTime.now());

        when(tokenValidationUtil.validateSellerToken(VALID_SELLER_TOKEN)).thenReturn(sellerSession);
        when(inventoryAlertDao.findBySeller_SellerId(SELLER_ID)).thenReturn(Arrays.asList(inventoryAlert, alert2));

        List<InventoryAlertResponseDTO> alerts = inventoryAlertService.getAllAlertsForSeller(VALID_SELLER_TOKEN);

        assertNotNull(alerts);
        assertEquals(2, alerts.size());
    }
}