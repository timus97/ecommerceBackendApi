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

import com.masai.dto.SellerDTO;
import com.masai.dto.SessionDTO;
import com.masai.models.Seller;
import com.masai.service.SellerService;

@DisplayName("SellerController Tests")
@ExtendWith(MockitoExtension.class)
class SellerControllerTest {

    @Mock
    private SellerService sellerService;

    @InjectMocks
    private SellerController sellerController;

    private Seller seller;
    private SellerDTO sellerDTO;

    @BeforeEach
    void setUp() {
        seller = new Seller();
        seller.setSellerId(1);
        seller.setFirstName("John");
        seller.setMobile("9876543210");

        sellerDTO = new SellerDTO();
        sellerDTO.setMobile("9876543210");
        sellerDTO.setPassword("password123");
    }

    @Test
    @DisplayName("Should add seller")
    void testAddSellerHandler() {
        when(sellerService.addSeller(any(Seller.class))).thenReturn(seller);

        ResponseEntity<Seller> response = sellerController.addSellerHandler(seller);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getSellerId());
    }

    @Test
    @DisplayName("Should get all sellers")
    void testGetAllSellerHandler() {
        when(sellerService.getAllSellers()).thenReturn(Arrays.asList(seller));

        ResponseEntity<List<Seller>> response = sellerController.getAllSellerHandler();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Should get seller by ID")
    void testGetSellerByIdHandler() {
        when(sellerService.getSellerById(1)).thenReturn(seller);

        ResponseEntity<Seller> response = sellerController.getSellerByIdHandler(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get seller by mobile")
    void testGetSellerByMobileHandler() {
        when(sellerService.getSellerByMobile(anyString(), anyString())).thenReturn(seller);

        ResponseEntity<Seller> response = sellerController.getSellerByMobileHandler("9876543210", "token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get currently logged in seller")
    void testGetLoggedInSellerHandler() {
        when(sellerService.getCurrentlyLoggedInSeller(anyString())).thenReturn(seller);

        ResponseEntity<Seller> response = sellerController.getLoggedInSellerHandler("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update seller")
    void testUpdateSellerHandler() {
        when(sellerService.updateSeller(any(Seller.class), anyString())).thenReturn(seller);

        ResponseEntity<Seller> response = sellerController.updateSellerHandler(seller, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update seller mobile")
    void testUpdateSellerMobileHandler() {
        when(sellerService.updateSellerMobile(any(SellerDTO.class), anyString())).thenReturn(seller);

        ResponseEntity<Seller> response = sellerController.updateSellerMobileHandler(sellerDTO, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update seller password")
    void testUpdateSellerPasswordHandler() {
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setMessage("Password updated");
        when(sellerService.updateSellerPassword(any(SellerDTO.class), anyString())).thenReturn(sessionDTO);

        ResponseEntity<SessionDTO> response = sellerController.updateSellerPasswordHandler(sellerDTO, "token");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should delete seller by ID")
    void testDeleteSellerByIdHandler() {
        when(sellerService.deleteSellerById(anyInt(), anyString())).thenReturn(seller);

        ResponseEntity<Seller> response = sellerController.deleteSellerByIdHandler(1, "token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
