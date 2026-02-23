package com.masai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.masai.models.Customer;
import com.masai.models.Product;
import com.masai.models.Review;

/**
 * Repository for Review entity with custom query for duplicate check.
 */
@Repository
public interface ReviewDao extends JpaRepository<Review, Long> {

    // Check if customer already reviewed this product
    boolean existsByCustomerAndProduct(Customer customer, Product product);

    // Find reviews for a product (excluding deleted), pageable
    Page<Review> findByProductIdAndIsDeletedFalse(Integer productId, Pageable pageable);

    // Find approved reviews for a product, pageable
    Page<Review> findByProductIdAndIsApprovedTrue(Integer productId, Pageable pageable);

    // Count reviews for a product
    long countByProductId(Integer productId);

    // Custom JPQL for average rating of approved/non-deleted reviews for a product
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId AND r.isDeleted = false AND r.isApproved = true")
    Double calculateAverageRating(@Param("productId") Integer productId);
}
