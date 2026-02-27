package com.masai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.masai.dto.ProductDTO;
import com.masai.dto.ProductSearchFilterDTO;
import com.masai.models.CategoryEnum;
import com.masai.models.Product;
import com.masai.models.ProductStatus;
import com.masai.service.ProductService;

@DisplayName("ProductController Tests")
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductId(1);
        product.setProductName("Test Product");
        product.setPrice(99.99);

        productDTO = new ProductDTO();
        productDTO.setProdName("Test Product");
        productDTO.setPrice(99.99);
    }

    @Test
    @DisplayName("Should add product to catalog")
    void testAddProductToCatalogHandler() {
        when(productService.addProductToCatalog(anyString(), any(Product.class))).thenReturn(product);

        ResponseEntity<Product> response = productController.addProductToCatalogHandler("token", product);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getProductId());
    }

    @Test
    @DisplayName("Should get product by ID")
    void testGetProductFromCatalogByIdHandler() {
        when(productService.getProductFromCatalogById(1)).thenReturn(product);

        ResponseEntity<Product> response = productController.getProductFromCatalogByIdHandler(1);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should delete product from catalog")
    void testDeleteProductFromCatalogHandler() {
        when(productService.deleteProductFromCatalog(1)).thenReturn("Product deleted");

        ResponseEntity<String> response = productController.deleteProductFromCatalogHandler(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Product deleted", response.getBody());
    }

    @Test
    @DisplayName("Should update product in catalog")
    void testUpdateProductInCatalogHandler() {
        when(productService.updateProductIncatalog(any(Product.class))).thenReturn(product);

        ResponseEntity<Product> response = productController.updateProductInCatalogHandler(product);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get all products")
    void testGetAllProductsHandler() {
        when(productService.getAllProductsIncatalog()).thenReturn(Arrays.asList(product));

        ResponseEntity<List<Product>> response = productController.getAllProductsHandler();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("Should get all products of seller")
    void testGetAllProductsOfSellerHandler() {
        when(productService.getAllProductsOfSeller(1)).thenReturn(Arrays.asList(productDTO));

        ResponseEntity<List<ProductDTO>> response = productController.getAllProductsOfSellerHandler(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get products by category")
    void testGetAllProductsInCategory() {
        when(productService.getProductsOfCategory(CategoryEnum.ELECTRONICS))
            .thenReturn(Arrays.asList(productDTO));

        ResponseEntity<List<ProductDTO>> response = productController.getAllProductsInCategory("ELECTRONICS");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should get products by status")
    void testGetProductsWithStatusHandler() {
        when(productService.getProductsOfStatus(ProductStatus.AVAILABLE))
            .thenReturn(Arrays.asList(productDTO));

        ResponseEntity<List<ProductDTO>> response = productController.getProductsWithStatusHandler("AVAILABLE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should update product quantity")
    void testUpdateQuantityOfProduct() {
        when(productService.updateProductQuantityWithId(eq(1), any(ProductDTO.class))).thenReturn(product);

        ResponseEntity<Product> response = productController.updateQuantityOfProduct(1, productDTO);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should search and filter products")
    void testSearchAndFilterProductsHandler() {
        Map<String, Object> results = new HashMap<>();
        results.put("content", Arrays.asList(productDTO));
        results.put("totalElements", 1);

        when(productService.searchAndFilterProducts(any(ProductSearchFilterDTO.class))).thenReturn(results);

        ResponseEntity<Map<String, Object>> response = productController.searchAndFilterProductsHandler(
            "test", "ELECTRONICS", "AVAILABLE", 10.0, 100.0, 4.0, "Test", 1, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
