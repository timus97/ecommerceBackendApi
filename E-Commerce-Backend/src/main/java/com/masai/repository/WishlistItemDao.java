package com.masai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.masai.models.WishlistItem;

@Repository
public interface WishlistItemDao extends JpaRepository<WishlistItem, Integer> {

    /**
     * Find all items belonging to a wishlist, sorted by addedAt descending
     * (most recently added first).
     */
    List<WishlistItem> findByWishlist_WishlistIdOrderByAddedAtDesc(Integer wishlistId);

    /**
     * Check whether a specific product is already in a specific wishlist.
     */
    Optional<WishlistItem> findByWishlist_WishlistIdAndProduct_ProductId(Integer wishlistId, Integer productId);
}
