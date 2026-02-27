package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.masai.dto.SellerDTO;
import com.masai.dto.SessionDTO;
import com.masai.exception.SellerException;
import com.masai.models.Seller;
import com.masai.models.UserSession;
import com.masai.repository.SellerRepository;
import com.masai.util.PasswordEncoderUtil;
import com.masai.util.TokenValidationUtil;

@DisplayName("SellerServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class SellerServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private LoginLogoutService loginService;

    @Mock
    private TokenValidationUtil tokenValidationUtil;

    @Mock
    private PasswordEncoderUtil passwordEncoderUtil;

    @InjectMocks
    private SellerServiceImpl sellerService;

    private Seller seller;
    private SellerDTO sellerDTO;
    private UserSession userSession;

    @BeforeEach
    void setUp() {
        seller = new Seller();
        seller.setSellerId(1);
        seller.setFirstName("John");
        seller.setLastName("Doe");
        seller.setMobile("9876543210");
        seller.setEmailId("john@example.com");
        seller.setPassword("password123");

        sellerDTO = new SellerDTO();
        sellerDTO.setMobile("9876543210");
        sellerDTO.setPassword("password123");

        userSession = new UserSession();
        userSession.setUserId(1);
        userSession.setToken("seller_token");
    }

    @Test
    @DisplayName("Should add seller successfully")
    void testAddSeller_Success() {
        when(sellerRepository.findByMobile(anyString())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encodePassword(anyString())).thenReturn("hashed_password");
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);

        Seller result = sellerService.addSeller(seller);

        assertNotNull(result);
        assertEquals(1, result.getSellerId());
    }

    @Test
    @DisplayName("Should throw exception when seller with mobile already exists")
    void testAddSeller_DuplicateMobile() {
        when(sellerRepository.findByMobile(anyString())).thenReturn(Optional.of(seller));

        assertThrows(SellerException.class, () ->
            sellerService.addSeller(seller));
    }

    @Test
    @DisplayName("Should get all sellers successfully")
    void testGetAllSellers_Success() {
        when(sellerRepository.findAll()).thenReturn(Arrays.asList(seller));

        var result = sellerService.getAllSellers();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw exception when no sellers found")
    void testGetAllSellers_Empty() {
        when(sellerRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(SellerException.class, () ->
            sellerService.getAllSellers());
    }

    @Test
    @DisplayName("Should get seller by ID successfully")
    void testGetSellerById_Success() {
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));

        Seller result = sellerService.getSellerById(1);

        assertNotNull(result);
        assertEquals(1, result.getSellerId());
    }

    @Test
    @DisplayName("Should throw exception when seller not found by ID")
    void testGetSellerById_NotFound() {
        when(sellerRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(SellerException.class, () ->
            sellerService.getSellerById(1));
    }

    @Test
    @DisplayName("Should update seller successfully")
    void testUpdateSeller_Success() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);

        Seller result = sellerService.updateSeller(seller, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should delete seller successfully")
    void testDeleteSellerById_Success() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        doNothing().when(sellerRepository).delete(any(Seller.class));
        when(loginService.logoutSeller(any(SessionDTO.class))).thenReturn(new SessionDTO());

        Seller result = sellerService.deleteSellerById(1, "token");

        assertNotNull(result);
        verify(sellerRepository).delete(seller);
    }

    @Test
    @DisplayName("Should throw exception when deleting seller with wrong ID")
    void testDeleteSellerById_WrongId() {
        UserSession wrongSession = new UserSession();
        wrongSession.setUserId(2);
        
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(wrongSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));

        assertThrows(SellerException.class, () ->
            sellerService.deleteSellerById(1, "token"));
    }

    @Test
    @DisplayName("Should update seller mobile successfully")
    void testUpdateSellerMobile_Success() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(passwordEncoderUtil.matchesPassword(anyString(), anyString())).thenReturn(true);
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);

        Seller result = sellerService.updateSellerMobile(sellerDTO, "token");

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw exception when password doesn't match for mobile update")
    void testUpdateSellerMobile_WrongPassword() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(passwordEncoderUtil.matchesPassword(anyString(), anyString())).thenReturn(false);

        assertThrows(SellerException.class, () ->
            sellerService.updateSellerMobile(sellerDTO, "token"));
    }

    @Test
    @DisplayName("Should get seller by mobile successfully")
    void testGetSellerByMobile_Success() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findByMobile(anyString())).thenReturn(Optional.of(seller));

        Seller result = sellerService.getSellerByMobile("9876543210", "token");

        assertNotNull(result);
        assertEquals("9876543210", result.getMobile());
    }

    @Test
    @DisplayName("Should throw exception when seller not found by mobile")
    void testGetSellerByMobile_NotFound() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findByMobile(anyString())).thenReturn(Optional.empty());

        assertThrows(SellerException.class, () ->
            sellerService.getSellerByMobile("9876543210", "token"));
    }

    @Test
    @DisplayName("Should get currently logged in seller successfully")
    void testGetCurrentlyLoggedInSeller_Success() {
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));

        Seller result = sellerService.getCurrentlyLoggedInSeller("token");

        assertNotNull(result);
        assertEquals(1, result.getSellerId());
    }

    @Test
    @DisplayName("Should update seller password successfully")
    void testUpdateSellerPassword_Success() {
        sellerDTO.setMobile("9876543210");
        
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(passwordEncoderUtil.encodePassword(anyString())).thenReturn("new_hashed_password");
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);
        when(loginService.logoutSeller(any(SessionDTO.class))).thenReturn(new SessionDTO());

        SessionDTO result = sellerService.updateSellerPassword(sellerDTO, "token");

        assertNotNull(result);
        assertEquals("Updated password and logged out. Login again with new password", result.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when mobile doesn't match for password update")
    void testUpdateSellerPassword_WrongMobile() {
        sellerDTO.setMobile("9999999999");
        
        when(tokenValidationUtil.validateSellerToken(anyString())).thenReturn(userSession);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));

        assertThrows(SellerException.class, () ->
            sellerService.updateSellerPassword(sellerDTO, "token"));
    }
}
