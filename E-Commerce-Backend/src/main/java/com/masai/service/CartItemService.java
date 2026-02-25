package com.masai.service;

import com.masai.dto.CartDTO;
import com.masai.models.CartItem;

public interface CartItemService {
	
	public CartItem createItemforCart(CartDTO cartdto);
	
}
