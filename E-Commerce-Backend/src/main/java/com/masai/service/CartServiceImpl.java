package com.masai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.exception.CartItemNotFound;
import com.masai.exception.CustomerNotFoundException;
import com.masai.models.Cart;
import com.masai.dto.CartDTO;
import com.masai.models.CartItem;
import com.masai.models.Customer;
import com.masai.models.UserSession;
import com.masai.repository.CartRepository;
import com.masai.repository.CustomerRepository;
import com.masai.util.TokenValidationUtil;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private TokenValidationUtil tokenValidationUtil;
	
	@Autowired
	private CartItemService cartItemService;
	
	
	@Autowired
	private CustomerRepository customerRepository;
	

	
	

	@Override
	@Transactional
	public Cart addProductToCart(CartDTO cartDto, String token) {

		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		Cart customerCart = existingCustomer.getCustomerCart();
		
		List<CartItem> cartItems = customerCart.getCartItems();
		
		CartItem item = cartItemService.createItemforCart(cartDto);
		
		
		if(cartItems.size() == 0) {
			cartItems.add(item);
			customerCart.setCartTotal(item.getCartProduct().getPrice());
		}
		else {
			boolean flag = false;
			for(CartItem c: cartItems) {
				if(c.getCartProduct().getProductId() == cartDto.getProductId()) {
					c.setCartItemQuantity(c.getCartItemQuantity() + 1);
					customerCart.setCartTotal(customerCart.getCartTotal() + c.getCartProduct().getPrice());
					flag = true;
				}
			}
			if(!flag) {
				cartItems.add(item);
				customerCart.setCartTotal(customerCart.getCartTotal() + item.getCartProduct().getPrice());
			}
		}
		
		return cartRepository.save(existingCustomer.getCustomerCart());
		

}
	
	

	@Override
	public Cart getCartProduct(String token) {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Optional<Customer> opt = customerRepository.findById(user.getUserId());
		
		
		if(opt.isEmpty())
			throw new CustomerNotFoundException("Customer does not exist");
		
		Customer existingCustomer = opt.get();
		
		Integer cartId = existingCustomer.getCustomerCart().getCartId();
		
		
		return cartRepository.findById(cartId)
				.orElseThrow(() -> new CartItemNotFound("cart Not found by Id"));
	}

	
	
	@Override
	@Transactional
	public Cart removeProductFromCart(CartDTO cartDto, String token) {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		Cart customerCart = existingCustomer.getCustomerCart();
		
		List<CartItem> cartItems = customerCart.getCartItems();
		
		if(cartItems.size() == 0) {
			throw new CartItemNotFound("Cart is empty");
		}
		
		
		boolean flag = false;
		
		for(CartItem c: cartItems) {
			if(c.getCartProduct().getProductId() == cartDto.getProductId()) {
				c.setCartItemQuantity(c.getCartItemQuantity() - 1);
				
				customerCart.setCartTotal(customerCart.getCartTotal() - c.getCartProduct().getPrice());
				if(c.getCartItemQuantity() == 0) {
					
					cartItems.remove(c);

					
					return cartRepository.save(customerCart);
				}
				flag = true;
			}
		}
		
		if(!flag) {
			throw new CartItemNotFound("Product not added to cart");
		}
		
		if(cartItems.size() == 0) {
			cartRepository.save(customerCart);
			throw new CartItemNotFound("Cart is empty now");
		}
		
		return cartRepository.save(customerCart);
	}
	
	@Override
	@Transactional
	public Cart clearCart(String token) {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		Cart customerCart = existingCustomer.getCustomerCart();
		
		if(customerCart.getCartItems().size() == 0) {
			throw new CartItemNotFound("Cart already empty");
		}
		
		List<CartItem> emptyCart = new ArrayList<>();
		
		customerCart.setCartItems(emptyCart);
		
		customerCart.setCartTotal(0.0);
		
		return cartRepository.save(customerCart);
	}
	
}
