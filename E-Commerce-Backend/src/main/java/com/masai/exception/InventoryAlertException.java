package com.masai.exception;

/**
 * Custom exception for inventory alert related errors
 */
public class InventoryAlertException extends RuntimeException {

    public InventoryAlertException() {
        super();
    }

    public InventoryAlertException(String message) {
        super(message);
    }

    public InventoryAlertException(String message, Throwable cause) {
        super(message, cause);
    }
}