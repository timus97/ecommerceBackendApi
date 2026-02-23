package com.masai.exception;

/**
 * Thrown for wishlist-specific business rule violations
 * (e.g. duplicate product, item not found in wishlist).
 */
public class WishlistException extends RuntimeException {

    public WishlistException() {
        super();
    }

    public WishlistException(String message) {
        super(message);
    }
}
