package com.masai.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.masai.models.CategoryEnum;
import com.masai.models.Product;
import com.masai.dto.ProductDTO;
import com.masai.dto.ProductSearchResponseDTO;
import com.masai.models.ProductStatus;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
	
	
	@Query("select new com.masai.dto.ProductDTO(p.productName,p.manufacturer,p.price,p.quantity) "
			+ "from Product p where p.category=:catenum")
	public List<ProductDTO> getAllProductsInACategory(@Param("catenum") CategoryEnum catenum);
	
	
	@Query("select new com.masai.dto.ProductDTO(p.productName,p.manufacturer,p.price,p.quantity) "
			+ "from Product p where p.status=:status")
	public List<ProductDTO> getProductsWithStatus(@Param("status") ProductStatus status);
	
	@Query("select new com.masai.dto.ProductDTO(p.productName,p.manufacturer,p.price,p.quantity) "
			+ "from Product p where p.seller.sellerId=:id")
	public List<ProductDTO> getProductsOfASeller(@Param("id") Integer id);
	
	
	/**
	 * Search and filter products with optional criteria
	 * Supports keyword search in product name and description
	 * Supports filtering by category, status, price range, rating, manufacturer, and seller
	 */
	@Query("SELECT new com.masai.dto.ProductSearchResponseDTO("
			+ "p.productId, p.productName, p.price, p.description, p.manufacturer, "
			+ "p.quantity, CAST(p.category AS string), CAST(p.status AS string), "
			+ "p.averageRating, p.reviewCount, COALESCE(s.firstName, '') || ' ' || COALESCE(s.lastName, '')) "
			+ "FROM Product p "
			+ "LEFT JOIN p.seller s "
			+ "WHERE (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER('%' || :keyword || '%') "
			+ "  OR LOWER(COALESCE(p.description, '')) LIKE LOWER('%' || :keyword || '%')) "
			+ "AND (:category IS NULL OR CAST(p.category AS string) = :category) "
			+ "AND (:status IS NULL OR CAST(p.status AS string) = :status) "
			+ "AND (:minPrice IS NULL OR p.price >= :minPrice) "
			+ "AND (:maxPrice IS NULL OR p.price <= :maxPrice) "
			+ "AND (:minRating IS NULL OR p.averageRating >= :minRating) "
			+ "AND (:manufacturer IS NULL OR LOWER(p.manufacturer) LIKE LOWER('%' || :manufacturer || '%')) "
			+ "AND (:sellerId IS NULL OR s.sellerId = :sellerId)")
	public Page<ProductSearchResponseDTO> searchAndFilterProducts(
			@Param("keyword") String keyword,
			@Param("category") String category,
			@Param("status") String status,
			@Param("minPrice") Double minPrice,
			@Param("maxPrice") Double maxPrice,
			@Param("minRating") Double minRating,
			@Param("manufacturer") String manufacturer,
			@Param("sellerId") Integer sellerId,
			Pageable pageable);

}
