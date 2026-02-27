package com.masai.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.exception.CustomerException;
import com.masai.exception.CustomerNotFoundException;
import com.masai.exception.LoginException;
import com.masai.models.Address;
import com.masai.models.Cart;
import com.masai.models.CreditCard;
import com.masai.models.Customer;
import com.masai.dto.CustomerDTO;
import com.masai.dto.CustomerUpdateDTO;
import com.masai.models.Order;
import com.masai.dto.SessionDTO;
import com.masai.models.UserSession;
import com.masai.models.Wishlist;
import com.masai.repository.CustomerRepository;
import com.masai.repository.WishlistRepository;
import com.masai.util.PasswordEncoderUtil;
import com.masai.util.TokenValidationUtil;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerServiceImpl implements CustomerService{
	
	@Autowired
	private CustomerRepository customerRepository;
	
	@Autowired
	private LoginLogoutService loginService;
	
	@Autowired
	private TokenValidationUtil tokenValidationUtil;

	@Autowired
	private WishlistRepository wishlistRepository;
	
	@Autowired
	private PasswordEncoderUtil passwordEncoderUtil;
	
	
	// Method to add a new customer
	
	@Override
	@Transactional
	public Customer addCustomer(Customer customer) {
				
		customer.setCreatedOn(LocalDateTime.now());
		
		// Hash the password using bcrypt
		String hashedPassword = passwordEncoderUtil.encodePassword(customer.getPassword());
		customer.setPassword(hashedPassword);
		
		Cart c = new Cart();
		customer.setCustomerCart(c);

		Wishlist wishlist = new Wishlist();
		wishlist.setCustomer(customer);
		customer.setCustomerWishlist(wishlist);
		
		customer.setOrders(new ArrayList<Order>());

		Optional<Customer> existing = customerRepository.findByMobileNo(customer.getMobileNo());
		
		if(existing.isPresent())
			throw new CustomerException("Customer already exists. Please try to login with your mobile no");
		
		customerRepository.save(customer);
		
		return customer;
	}

	
	
	// Method to get a customer by mobile number
	
	@Override
	public Customer getLoggedInCustomerDetails(String token){
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		return existingCustomer;
	}
	
	

	
	// Method to get all customers - only seller or admin can get all customers - check validity of seller token

	@Override
	public List<Customer> getAllCustomers(String token) throws CustomerNotFoundException {
		
		// update to seller
		
		tokenValidationUtil.validateSellerToken(token);
		
		List<Customer> customers = customerRepository.findAll();
		
		if(customers.size() == 0)
			throw new CustomerNotFoundException("No record exists");
		
		return customers;
	}


	// Method to update entire customer details - either mobile number or email id should be correct
	
	@Override
	@Transactional
	public Customer updateCustomer(CustomerUpdateDTO customer, String token) throws CustomerNotFoundException {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Optional<Customer> opt = customerRepository.findByMobileNo(customer.getMobileNo());
		
		Optional<Customer> res = customerRepository.findByEmailId(customer.getEmailId());
		
		if(opt.isEmpty() && res.isEmpty())
			throw new CustomerNotFoundException("Customer does not exist with given mobile no or email-id");
		
		Customer existingCustomer = null;
		
		if(opt.isPresent())
			existingCustomer = opt.get();
		else
			existingCustomer = res.get();
		
		if(existingCustomer.getCustomerId() == user.getUserId()) {
		
			if(customer.getFirstName() != null) {
				existingCustomer.setFirstName(customer.getFirstName());
			}
			
			if(customer.getLastName() != null) {
				existingCustomer.setLastName(customer.getLastName());
			}
			
			if(customer.getEmailId() != null) {
				existingCustomer.setEmailId(customer.getEmailId());
			}
			
			if(customer.getMobileNo() != null) {
				existingCustomer.setMobileNo(customer.getMobileNo());
			}
			
			if(customer.getPassword() != null) {
				// Hash the password using bcrypt
			String hashedPassword = passwordEncoderUtil.encodePassword(customer.getPassword());
			existingCustomer.setPassword(hashedPassword);
			}
			
			if(customer.getAddress() != null) {			
				for(Map.Entry<String, Address> values : customer.getAddress().entrySet()) {
					existingCustomer.getAddress().put(values.getKey(), values.getValue());
				}
			}
			
			customerRepository.save(existingCustomer);
			return existingCustomer;
		
		}
		else {
			throw new CustomerException("Error in updating. Verification failed.");
		}
		
		
	}

	
	// Method to update customer mobile number - details updated for current logged in user

	@Override
	@Transactional
	public Customer updateCustomerMobileNoOrEmailId(CustomerUpdateDTO customerUpdateDTO, String token) throws CustomerNotFoundException {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		if(customerUpdateDTO.getEmailId() != null) {
			existingCustomer.setEmailId(customerUpdateDTO.getEmailId());
		}
		
		
		existingCustomer.setMobileNo(customerUpdateDTO.getMobileNo());
			
		customerRepository.save(existingCustomer);
			
		return existingCustomer;
		
	}

	// Method to update password - based on current token
	
	@Override
	@Transactional
	public SessionDTO updateCustomerPassword(CustomerDTO customerDTO, String token) {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		
		if(customerDTO.getMobileId().equals(existingCustomer.getMobileNo()) == false) {
			throw new CustomerException("Verification error. Mobile number does not match");
		}
		
		// Hash the password using bcrypt
	String hashedPassword = passwordEncoderUtil.encodePassword(customerDTO.getPassword());
	existingCustomer.setPassword(hashedPassword);
		
		customerRepository.save(existingCustomer);
		
		SessionDTO session = new SessionDTO();
		
		session.setToken(token);
		
		loginService.logoutCustomer(session);
		
		session.setMessage("Updated password and logged out. Login again with new password");
		
		return session;

	}
	
	
	// Method to add/update Address
	
	
	@Override
	@Transactional
	public Customer updateAddress(Address address, String type, String token) throws CustomerException {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		existingCustomer.getAddress().put(type, address);
		
		return customerRepository.save(existingCustomer);
		
	}
	
	
	// Method to update Credit card
	
	@Override
	@Transactional
	public Customer updateCreditCardDetails(String token, CreditCard card) throws CustomerException{
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		existingCustomer.setCreditCard(card);
		
		return customerRepository.save(existingCustomer);
	}
	
	
	
	// Method to delete a customer by mobile id
	
	@Override
	@Transactional
	public SessionDTO deleteCustomer(CustomerDTO customerDTO, String token) throws CustomerNotFoundException {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		SessionDTO session = new SessionDTO();
		
		session.setMessage("");

		session.setToken(token);
		
		if(existingCustomer.getMobileNo().equals(customerDTO.getMobileId()) 
				&& passwordEncoderUtil.matchesPassword(customerDTO.getPassword(), existingCustomer.getPassword())) {
			
			customerRepository.delete(existingCustomer);
			
			loginService.logoutCustomer(session);
			
			session.setMessage("Deleted account and logged out successfully");
			
			return session;
		}
		else {
			throw new CustomerException("Verification error in deleting account. Please re-check details");
		}

	}



	@Override
	@Transactional
	public Customer deleteAddress(String type, String token) throws CustomerException, CustomerNotFoundException {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		if(existingCustomer.getAddress().containsKey(type) == false)
			throw new CustomerException("Address type does not exist");
		
		existingCustomer.getAddress().remove(type);
		
		return customerRepository.save(existingCustomer);
	}



	@Override
	public List<Order> getCustomerOrders(String token) throws CustomerException {
		
		UserSession user = tokenValidationUtil.validateCustomerToken(token);
		
		Customer existingCustomer = customerRepository.findById(user.getUserId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer does not exist"));
		
		List<Order> myOrders = existingCustomer.getOrders();
		
		if(myOrders.size() == 0)
			throw new CustomerException("No orders found");
		
		return myOrders;
	}



	
	
	
	

}
