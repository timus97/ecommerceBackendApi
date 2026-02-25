package com.masai.service;

import com.masai.exception.CartItemNotFound;
import com.masai.models.Cart;
import com.masai.dto.CartDTO;




public interface CartService {
	
	public Cart addProductToCart(CartDTO cart, String token) throws CartItemNotFound;
	public Cart getCartProduct(String token);
	public Cart removeProductFromCart(CartDTO cartDto,String token) throws CartItemNotFound;
	
	public Cart clearCart(String token);
	
}
