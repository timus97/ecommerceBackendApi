package com.masai.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.masai.models.Cart;
import com.masai.models.WishlistResponseDTO;
import com.masai.service.WishlistService;

/**
 * REST controller exposing all wishlist endpoints.
 * All routes require a valid customer session token supplied via the "token" header.
 */
@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    /**
     * POST /wishlist/{productId}
     * Add a product to the authenticated customer's wishlist.
     * Returns 201 CREATED with the saved wishlist item details.
     * Returns 400 BAD REQUEST if the product is already wishlisted.
     */
    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponseDTO> addToWishlist(
            @PathVariable Integer productId,
            @RequestHeader("token") String token) {

        WishlistResponseDTO saved = wishlistService.addToWishlist(productId, token);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    /**
     * GET /wishlist
     * Retrieve all wishlisted products for the authenticated customer,
     * sorted by most recently added first.
     */
    @GetMapping
    public ResponseEntity<List<WishlistResponseDTO>> getWishlist(
            @RequestHeader("token") String token) {

        List<WishlistResponseDTO> items = wishlistService.getWishlist(token);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    /**
     * DELETE /wishlist/{productId}
     * Remove a single product from the customer's wishlist.
     * Returns 200 OK with a confirmation message.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, String>> removeFromWishlist(
            @PathVariable Integer productId,
            @RequestHeader("token") String token) {

        String message = wishlistService.removeFromWishlist(productId, token);
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.OK);
    }

    /**
     * POST /wishlist/{productId}/move-to-cart
     * Atomically removes the product from the wishlist and adds it to the cart.
     * Returns 200 OK with the updated cart.
     */
    @PostMapping("/{productId}/move-to-cart")
    public ResponseEntity<Cart> moveToCart(
            @PathVariable Integer productId,
            @RequestHeader("token") String token) {

        Cart updatedCart = wishlistService.moveToCart(productId, token);
        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    /**
     * GET /wishlist/{productId}/check
     * Returns {"wishlisted": true/false} indicating whether the product
     * is currently in the customer's wishlist.
     */
    @GetMapping("/{productId}/check")
    public ResponseEntity<Map<String, Boolean>> isWishlisted(
            @PathVariable Integer productId,
            @RequestHeader("token") String token) {

        boolean result = wishlistService.isWishlisted(productId, token);
        return new ResponseEntity<>(Map.of("wishlisted", result), HttpStatus.OK);
    }
}
