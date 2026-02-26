package com.masai.service;

import java.util.List;
import java.util.Map;

import com.masai.models.CategoryEnum;
import com.masai.models.Product;
import com.masai.dto.ProductDTO;
import com.masai.dto.ProductSearchFilterDTO;
import com.masai.models.ProductStatus;

public interface ProductService {

	public Product addProductToCatalog(String token, Product product);

	public Product getProductFromCatalogById(Integer id);

	public String deleteProductFromCatalog(Integer id);

	public Product updateProductIncatalog(Product product);
	
	public List<Product> getAllProductsIncatalog();
	
	public List<ProductDTO> getAllProductsOfSeller(Integer id);
	
	public List<ProductDTO> getProductsOfCategory(CategoryEnum catenum);
	
	public List<ProductDTO> getProductsOfStatus(ProductStatus status);
	
	
	
	public Product updateProductQuantityWithId(Integer id,ProductDTO prodDTO);
	
	/**
	 * Search and filter products with multiple criteria
	 * @param filterDTO contains search keyword and filter criteria
	 * @return Map containing search results and pagination metadata
	 */
	public Map<String, Object> searchAndFilterProducts(ProductSearchFilterDTO filterDTO);

}
