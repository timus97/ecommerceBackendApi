package com.masai.exception;

public class CartItemNotFound extends RuntimeException{

	public CartItemNotFound() {
		super();
	}
	
	public CartItemNotFound(String message) {
		super(message);
	}
}
