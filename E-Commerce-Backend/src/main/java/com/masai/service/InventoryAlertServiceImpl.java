package com.masai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Implementation of InventoryAlertService
 */
@Service
public class InventoryAlertServiceImpl implements InventoryAlertService {

    @Autowired
    private InventoryAlertDao inventoryAlertDao;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private TokenValidationUtil tokenValidationUtil;

    @Override
    @Transactional
    public InventoryAlertResponseDTO createAlert(String token, InventoryAlertRequestDTO requestDTO) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Seller seller = sellerService.getSellerById(session.getUserId());

        // Validate product exists and belongs to seller
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + requestDTO.getProductId()));

        // Verify product belongs to the seller
        if (!product.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new InventoryAlertException("You can only set alerts for your own products");
        }

        // Check if alert already exists for this product
        if (inventoryAlertDao.existsByProduct_ProductId(requestDTO.getProductId())) {
            throw new InventoryAlertException("Alert already exists for this product. Use update instead.");
        }

        // Create new alert
        InventoryAlert alert = new InventoryAlert();
        alert.setProduct(product);
        alert.setSeller(seller);
        alert.setThresholdQuantity(requestDTO.getThresholdQuantity());
        alert.setAlertEnabled(requestDTO.getAlertEnabled() != null ? requestDTO.getAlertEnabled() : true);
        alert.prePersist();

        InventoryAlert savedAlert = inventoryAlertDao.save(alert);

        return convertToResponseDTO(savedAlert);
    }

    @Override
    @Transactional
    public InventoryAlertResponseDTO updateAlert(String token, Integer alertId, InventoryAlertRequestDTO requestDTO) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Seller seller = sellerService.getSellerById(session.getUserId());

        // Find existing alert
        InventoryAlert alert = inventoryAlertDao.findById(alertId)
                .orElseThrow(() -> new InventoryAlertException("Alert not found with id: " + alertId));

        // Verify alert belongs to the seller
        if (!alert.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new InventoryAlertException("You can only update your own alerts");
        }

        // Validate product if changed
        if (!alert.getProduct().getProductId().equals(requestDTO.getProductId())) {
            Product newProduct = productRepository.findById(requestDTO.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + requestDTO.getProductId()));

            if (!newProduct.getSeller().getSellerId().equals(seller.getSellerId())) {
                throw new InventoryAlertException("You can only set alerts for your own products");
            }

            // Check if alert already exists for the new product
            Optional<InventoryAlert> existingAlert = inventoryAlertDao.findByProduct_ProductId(requestDTO.getProductId());
            if (existingAlert.isPresent() && !existingAlert.get().getAlertId().equals(alertId)) {
                throw new InventoryAlertException("Alert already exists for the target product");
            }

            alert.setProduct(newProduct);
        }

        // Update alert details
        alert.setThresholdQuantity(requestDTO.getThresholdQuantity());
        if (requestDTO.getAlertEnabled() != null) {
            alert.setAlertEnabled(requestDTO.getAlertEnabled());
        }
        alert.preUpdate();

        InventoryAlert updatedAlert = inventoryAlertDao.save(alert);

        return convertToResponseDTO(updatedAlert);
    }

    @Override
    @Transactional
    public String deleteAlert(String token, Integer alertId) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Seller seller = sellerService.getSellerById(session.getUserId());

        // Find alert
        InventoryAlert alert = inventoryAlertDao.findById(alertId)
                .orElseThrow(() -> new InventoryAlertException("Alert not found with id: " + alertId));

        // Verify alert belongs to the seller
        if (!alert.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new InventoryAlertException("You can only delete your own alerts");
        }

        inventoryAlertDao.delete(alert);
        return "Alert deleted successfully";
    }

    @Override
    public List<InventoryAlertResponseDTO> getAllAlertsForSeller(String token) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Integer sellerId = session.getUserId();

        List<InventoryAlert> alerts = inventoryAlertDao.findBySeller_SellerId(sellerId);

        return alerts.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryAlertResponseDTO> getEnabledAlertsForSeller(String token) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Integer sellerId = session.getUserId();

        List<InventoryAlert> alerts = inventoryAlertDao.findEnabledAlertsBySellerId(sellerId);

        return alerts.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryAlertSummaryDTO> getTriggeredAlertsForSeller(String token) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Integer sellerId = session.getUserId();

        List<InventoryAlert> triggeredAlerts = inventoryAlertDao.findTriggeredAlertsBySellerId(sellerId);

        return triggeredAlerts.stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InventoryAlertResponseDTO getAlertById(String token, Integer alertId) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Seller seller = sellerService.getSellerById(session.getUserId());

        InventoryAlert alert = inventoryAlertDao.findById(alertId)
                .orElseThrow(() -> new InventoryAlertException("Alert not found with id: " + alertId));

        // Verify alert belongs to the seller
        if (!alert.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new InventoryAlertException("You can only view your own alerts");
        }

        return convertToResponseDTO(alert);
    }

    @Override
    @Transactional
    public InventoryAlertResponseDTO toggleAlertStatus(String token, Integer alertId, Boolean enabled) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Seller seller = sellerService.getSellerById(session.getUserId());

        InventoryAlert alert = inventoryAlertDao.findById(alertId)
                .orElseThrow(() -> new InventoryAlertException("Alert not found with id: " + alertId));

        // Verify alert belongs to the seller
        if (!alert.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new InventoryAlertException("You can only modify your own alerts");
        }

        alert.setAlertEnabled(enabled);
        alert.preUpdate();

        InventoryAlert updatedAlert = inventoryAlertDao.save(alert);

        return convertToResponseDTO(updatedAlert);
    }

    @Override
    public List<InventoryAlertSummaryDTO> getAllTriggeredAlerts() {
        List<InventoryAlert> triggeredAlerts = inventoryAlertDao.findTriggeredAlerts();

        return triggeredAlerts.stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InventoryAlertResponseDTO getAlertByProductId(String token, Integer productId) {
        // Validate seller token
        UserSession session = tokenValidationUtil.validateSellerToken(token);
        Seller seller = sellerService.getSellerById(session.getUserId());

        // Validate product exists and belongs to seller
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        if (!product.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new InventoryAlertException("You can only view alerts for your own products");
        }

        InventoryAlert alert = inventoryAlertDao.findByProduct_ProductId(productId)
                .orElseThrow(() -> new InventoryAlertException("No alert configured for this product"));

        return convertToResponseDTO(alert);
    }

    /**
     * Convert InventoryAlert entity to InventoryAlertResponseDTO
     */
    private InventoryAlertResponseDTO convertToResponseDTO(InventoryAlert alert) {
        Product product = alert.getProduct();
        Seller seller = alert.getSeller();

        boolean isTriggered = alert.getAlertEnabled() && product.getQuantity() <= alert.getThresholdQuantity();

        return InventoryAlertResponseDTO.builder()
                .alertId(alert.getAlertId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .sellerId(seller.getSellerId())
                .sellerName(seller.getFirstName() + " " + seller.getLastName())
                .thresholdQuantity(alert.getThresholdQuantity())
                .currentQuantity(product.getQuantity())
                .alertEnabled(alert.getAlertEnabled())
                .alertTriggered(isTriggered)
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .lastAlertSentAt(alert.getLastAlertSentAt())
                .alertCount(alert.getAlertCount())
                .build();
    }

    /**
     * Convert InventoryAlert entity to InventoryAlertSummaryDTO
     */
    private InventoryAlertSummaryDTO convertToSummaryDTO(InventoryAlert alert) {
        Product product = alert.getProduct();
        int quantityToRestock = alert.getThresholdQuantity() - product.getQuantity() + 1;
        if (quantityToRestock < 0) quantityToRestock = 0;

        return InventoryAlertSummaryDTO.builder()
                .alertId(alert.getAlertId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .thresholdQuantity(alert.getThresholdQuantity())
                .currentQuantity(product.getQuantity())
                .quantityToRestock(quantityToRestock)
                .lastAlertSentAt(alert.getLastAlertSentAt())
                .build();
    }
}