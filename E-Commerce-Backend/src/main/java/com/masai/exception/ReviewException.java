package com.masai.exception;

/**
 * Custom exception for review operations (e.g., duplicate reviews).
 */
public class ReviewException extends RuntimeException {

    public ReviewException() {
    }

    public ReviewException(String message) {
        super(message);
    }
}
