package com.masai.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.masai.models.CategoryEnum;
import com.masai.models.Product;
import com.masai.dto.ProductDTO;
import com.masai.dto.ProductSearchFilterDTO;
import com.masai.models.ProductStatus;
import com.masai.service.ProductService;

import io.swagger.v3.oas.models.security.SecurityScheme.In;

@RestController
public class ProductController {

	@Autowired
	private ProductService pService;

	// this method adds new product to catalog by seller(if seller is new it adds
	// seller as well
	// if seller is already existing products will be mapped to same seller) and
	// returns added product

	@PostMapping("/products")
	public ResponseEntity<Product> addProductToCatalogHandler(@RequestHeader("token") String token,
			@Valid @RequestBody Product product) {

		Product prod = pService.addProductToCatalog(token, product);

		return new ResponseEntity<Product>(prod, HttpStatus.ACCEPTED);

	}

	// This method gets the product which needs to be added to the cart returns
	// product

	@GetMapping("/product/{id}")
	public ResponseEntity<Product> getProductFromCatalogByIdHandler(@PathVariable("id") Integer id) {

		Product prod = pService.getProductFromCatalogById(id);

		return new ResponseEntity<Product>(prod, HttpStatus.FOUND);

	}

	// This method will delete the product from catalog and returns the response
	// This will be called only when the product qty will be zero or seller wants to
	// delete for any other reason

	@DeleteMapping("/product/{id}")
	public ResponseEntity<String> deleteProductFromCatalogHandler(@PathVariable("id") Integer id) {
		
		String res = pService.deleteProductFromCatalog(id);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}

	@PutMapping("/products")
	public ResponseEntity<Product> updateProductInCatalogHandler(@Valid @RequestBody Product prod) {

		Product prod1 = pService.updateProductIncatalog(prod);

		return new ResponseEntity<Product>(prod1, HttpStatus.OK);

	}

	@GetMapping("/products")
	public ResponseEntity<List<Product>> getAllProductsHandler() {

		List<Product> list = pService.getAllProductsIncatalog();

		return new ResponseEntity<List<Product>>(list, HttpStatus.OK);
	}
	
  //this method gets the products mapped to a particular seller
	@GetMapping("/products/seller/{id}")
	public ResponseEntity<List<ProductDTO>> getAllProductsOfSellerHandler(@PathVariable("id") Integer id) {

		List<ProductDTO> list = pService.getAllProductsOfSeller(id);

		return new ResponseEntity<List<ProductDTO>>(list, HttpStatus.OK);
	}

	@GetMapping("/products/{catenum}")
	public ResponseEntity<List<ProductDTO>> getAllProductsInCategory(@PathVariable("catenum") String catenum) {
		CategoryEnum ce = CategoryEnum.valueOf(catenum.toUpperCase());
		List<ProductDTO> list = pService.getProductsOfCategory(ce);
		return new ResponseEntity<List<ProductDTO>>(list, HttpStatus.OK);

	}

	@GetMapping("/products/status/{status}")
	public ResponseEntity<List<ProductDTO>> getProductsWithStatusHandler(@PathVariable("status") String status) {

		ProductStatus ps = ProductStatus.valueOf(status.toUpperCase());
		List<ProductDTO> list = pService.getProductsOfStatus(ps);

		return new ResponseEntity<List<ProductDTO>>(list, HttpStatus.OK);

	}
	
	
	@PutMapping("/products/{id}")
	public ResponseEntity<Product> updateQuantityOfProduct(@PathVariable("id") Integer id,@RequestBody ProductDTO prodDto){
		
		 Product prod =   pService.updateProductQuantityWithId(id, prodDto);
		
		 return new ResponseEntity<Product>(prod,HttpStatus.ACCEPTED);
	}
	
	/**
	 * Search and filter products with multiple criteria
	 * Supports keyword search in product name and description
	 * Supports filtering by category, status, price range, rating, manufacturer, and seller
	 * 
	 * @param keyword Search keyword (optional) - searches in product name and description
	 * @param category Filter by category (optional) - e.g., ELECTRONICS, FASHION, BOOKS, FURNITURE, GROCERIES
	 * @param status Filter by status (optional) - e.g., AVAILABLE, OUTOFSTOCK
	 * @param minPrice Minimum price filter (optional)
	 * @param maxPrice Maximum price filter (optional)
	 * @param minRating Minimum average rating filter (optional)
	 * @param manufacturer Filter by manufacturer (optional)
	 * @param sellerId Filter by seller ID (optional)
	 * @param page Page number for pagination (default: 0)
	 * @param size Page size for pagination (default: 10)
	 * @return ResponseEntity containing search results with pagination metadata
	 */
	@GetMapping("/products/search/filter")
	public ResponseEntity<Map<String, Object>> searchAndFilterProductsHandler(
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "minPrice", required = false) Double minPrice,
			@RequestParam(value = "maxPrice", required = false) Double maxPrice,
			@RequestParam(value = "minRating", required = false) Double minRating,
			@RequestParam(value = "manufacturer", required = false) String manufacturer,
			@RequestParam(value = "sellerId", required = false) Integer sellerId,
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "10") Integer size) {
		
		// Create filter DTO from request parameters
		ProductSearchFilterDTO filterDTO = new ProductSearchFilterDTO();
		filterDTO.setKeyword(keyword);
		filterDTO.setCategory(category);
		filterDTO.setStatus(status);
		filterDTO.setMinPrice(minPrice);
		filterDTO.setMaxPrice(maxPrice);
		filterDTO.setMinRating(minRating);
		filterDTO.setManufacturer(manufacturer);
		filterDTO.setSellerId(sellerId);
		filterDTO.setPage(page);
		filterDTO.setSize(size);
		
		// Call service to get search results
		Map<String, Object> results = pService.searchAndFilterProducts(filterDTO);
		
		return new ResponseEntity<>(results, HttpStatus.OK);
	}

}
