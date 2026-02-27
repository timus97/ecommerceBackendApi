package com.masai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.masai.dto.ProductDTO;
import com.masai.dto.ProductSearchFilterDTO;
import com.masai.dto.ProductSearchResponseDTO;
import com.masai.exception.CategoryNotFoundException;
import com.masai.exception.ProductNotFoundException;
import com.masai.models.CategoryEnum;
import com.masai.models.Product;
import com.masai.models.ProductStatus;
import com.masai.models.Seller;
import com.masai.repository.ProductRepository;
import com.masai.repository.SellerRepository;

@DisplayName("ProductServiceImpl Tests")
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerService sellerService;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private Seller seller;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        seller = new Seller();
        seller.setSellerId(1);
        seller.setFirstName("John");
        seller.setLastName("Doe");
        seller.setMobile("9876543210");

        product = new Product();
        product.setProductId(1);
        product.setProductName("Test Product");
        product.setPrice(99.99);
        product.setQuantity(10);
        product.setManufacturer("Test Manufacturer");
        product.setCategory(CategoryEnum.ELECTRONICS);
        product.setStatus(ProductStatus.AVAILABLE);
        product.setSeller(seller);

        productDTO = new ProductDTO();
        productDTO.setProdName("Test Product");
        productDTO.setManufaturer("Test Manufacturer");
        productDTO.setPrice(99.99);
        productDTO.setQuantity(10);
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductFromCatalogById_Success() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        Product result = productService.getProductFromCatalogById(1);

        assertNotNull(result);
        assertEquals(1, result.getProductId());
        assertEquals("Test Product", result.getProductName());
    }

    @Test
    @DisplayName("Should throw exception when product not found by ID")
    void testGetProductFromCatalogById_NotFound() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
            productService.getProductFromCatalogById(1));
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProductFromCatalog_Success() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);

        String result = productService.deleteProductFromCatalog(1);

        assertEquals("Product deleted from catalog", result);
        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void testDeleteProductFromCatalog_NotFound() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
            productService.deleteProductFromCatalog(1));
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProductIncatalog_Success() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.updateProductIncatalog(product);

        assertNotNull(result);
        assertEquals(1, result.getProductId());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void testUpdateProductIncatalog_NotFound() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () ->
            productService.updateProductIncatalog(product));
    }

    @Test
    @DisplayName("Should get all products successfully")
    void testGetAllProductsIncatalog_Success() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(product));

        List<Product> result = productService.getAllProductsIncatalog();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw exception when no products in catalog")
    void testGetAllProductsIncatalog_Empty() {
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(ProductNotFoundException.class, () ->
            productService.getAllProductsIncatalog());
    }

    @Test
    @DisplayName("Should get products by category successfully")
    void testGetProductsOfCategory_Success() {
        when(productRepository.getAllProductsInACategory(CategoryEnum.ELECTRONICS))
            .thenReturn(Arrays.asList(productDTO));

        List<ProductDTO> result = productService.getProductsOfCategory(CategoryEnum.ELECTRONICS);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw exception when no products in category")
    void testGetProductsOfCategory_Empty() {
        when(productRepository.getAllProductsInACategory(CategoryEnum.ELECTRONICS))
            .thenReturn(Collections.emptyList());

        assertThrows(CategoryNotFoundException.class, () ->
            productService.getProductsOfCategory(CategoryEnum.ELECTRONICS));
    }

    @Test
    @DisplayName("Should get products by status successfully")
    void testGetProductsOfStatus_Success() {
        when(productRepository.getProductsWithStatus(ProductStatus.AVAILABLE))
            .thenReturn(Arrays.asList(productDTO));

        List<ProductDTO> result = productService.getProductsOfStatus(ProductStatus.AVAILABLE);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw exception when no products with status")
    void testGetProductsOfStatus_Empty() {
        when(productRepository.getProductsWithStatus(ProductStatus.AVAILABLE))
            .thenReturn(Collections.emptyList());

        assertThrows(ProductNotFoundException.class, () ->
            productService.getProductsOfStatus(ProductStatus.AVAILABLE));
    }

    @Test
    @DisplayName("Should update product quantity successfully")
    void testUpdateProductQuantityWithId_Success() {
        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setQuantity(5);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.updateProductQuantityWithId(1, updateDTO);

        assertNotNull(result);
        assertEquals(15, result.getQuantity()); // 10 + 5
        assertEquals(ProductStatus.AVAILABLE, result.getStatus());
    }

    @Test
    @DisplayName("Should get all products of seller successfully")
    void testGetAllProductsOfSeller_Success() {
        when(productRepository.getProductsOfASeller(1)).thenReturn(Arrays.asList(productDTO));

        List<ProductDTO> result = productService.getAllProductsOfSeller(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should throw exception when no products for seller")
    void testGetAllProductsOfSeller_Empty() {
        when(productRepository.getProductsOfASeller(1)).thenReturn(Collections.emptyList());

        assertThrows(ProductNotFoundException.class, () ->
            productService.getAllProductsOfSeller(1));
    }

    @Test
    @DisplayName("Should search and filter products successfully")
    void testSearchAndFilterProducts_Success() {
        ProductSearchFilterDTO filterDTO = new ProductSearchFilterDTO();
        filterDTO.setKeyword("test");
        filterDTO.setPage(0);
        filterDTO.setSize(10);

        ProductSearchResponseDTO responseDTO = new ProductSearchResponseDTO();
        responseDTO.setProductId(1);
        responseDTO.setProductName("Test Product");

        Page<ProductSearchResponseDTO> page = new PageImpl<>(Arrays.asList(responseDTO));

        when(productRepository.searchAndFilterProducts(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(page);

        Map<String, Object> result = productService.searchAndFilterProducts(filterDTO);

        assertNotNull(result);
        assertTrue(result.containsKey("content"));
        assertTrue(result.containsKey("totalElements"));
    }

    @Test
    @DisplayName("Should throw exception when no products match search criteria")
    void testSearchAndFilterProducts_Empty() {
        ProductSearchFilterDTO filterDTO = new ProductSearchFilterDTO();
        filterDTO.setKeyword("nonexistent");

        Page<ProductSearchResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());

        when(productRepository.searchAndFilterProducts(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(emptyPage);

        assertThrows(ProductNotFoundException.class, () ->
            productService.searchAndFilterProducts(filterDTO));
    }
}
