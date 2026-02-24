package com.masai.service;

import java.util.List;

import com.masai.models.Cart;
import com.masai.models.WishlistResponseDTO;

/**
 * Service contract for all wishlist operations.
 */
public interface WishlistService {

    /**
     * Add a product to the customer's wishlist.
     * Throws WishlistException if the product is already wishlisted.
     */
    WishlistResponseDTO addToWishlist(Integer productId, String token);

    /**
     * Return all wishlisted products for the customer, sorted by most recently added.
     */
    List<WishlistResponseDTO> getWishlist(String token);

    /**
     * Remove a single product from the customer's wishlist.
     * Throws WishlistException if the product is not in the wishlist.
     */
    String removeFromWishlist(Integer productId, String token);

    /**
     * Move a product from the wishlist to the cart in a single atomic call.
     * Removes the wishlist entry and adds the product to the cart.
     */
    Cart moveToCart(Integer productId, String token);

    /**
     * Returns true if the product is currently in the customer's wishlist.
     */
    boolean isWishlisted(Integer productId, String token);
}
