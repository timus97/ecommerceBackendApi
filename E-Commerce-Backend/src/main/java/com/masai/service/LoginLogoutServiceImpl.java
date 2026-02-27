package com.masai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.exception.CustomerNotFoundException;
import com.masai.exception.LoginException;
import com.masai.exception.SellerNotFoundException;
import com.masai.models.Customer;
import com.masai.dto.CustomerDTO;
import com.masai.models.Seller;
import com.masai.dto.SellerDTO;
import com.masai.dto.SessionDTO;
import com.masai.models.UserSession;
import com.masai.repository.CustomerRepository;
import com.masai.repository.SellerRepository;
import com.masai.repository.SessionRepository;
import com.masai.util.PasswordEncoderUtil;
import com.masai.util.TokenValidationUtil;

@Service
public class LoginLogoutServiceImpl implements LoginLogoutService{

	
	@Autowired
	private SessionRepository sessionRepository;
	
	@Autowired
	private CustomerRepository customerRepository;
	
	@Autowired
	private SellerRepository sellerRepository;
	
	@Autowired
	private PasswordEncoderUtil passwordEncoderUtil;
	
	@Autowired
	private TokenValidationUtil tokenValidationUtil;

 
	
	// Method to login a customer

	@Override
	public UserSession loginCustomer(CustomerDTO loginCustomer) {
		
		Customer existingCustomer = customerRepository.findByMobileNo(loginCustomer.getMobileId())
				.orElseThrow(() -> new CustomerNotFoundException("Customer record does not exist with given mobile number"));
		
		Optional<UserSession> opt = sessionRepository.findByUserId(existingCustomer.getCustomerId());
		
		if(opt.isPresent()) {
			
			UserSession user = opt.get();
			
			if(user.getSessionEndTime().isBefore(LocalDateTime.now())) {
				sessionRepository.delete(user);	
			}
			else
				throw new LoginException("User already logged in");
			
		}
		
		
		if(passwordEncoderUtil.matchesPassword(loginCustomer.getPassword(), existingCustomer.getPassword())) {
		
			UserSession newSession = new UserSession();
			
			newSession.setUserId(existingCustomer.getCustomerId());
			newSession.setUserType("customer");
			newSession.setSessionStartTime(LocalDateTime.now());
			newSession.setSessionEndTime(LocalDateTime.now().plusHours(TokenValidationUtil.SESSION_DURATION_HOURS));
			
			UUID uuid = UUID.randomUUID();
			String token = TokenValidationUtil.CUSTOMER_PREFIX + uuid.toString().split("-")[0];
			
			newSession.setToken(token);
			
			return sessionRepository.save(newSession);
		}
		else {
			throw new LoginException("Password Incorrect. Try again.");
		}
	}

	
	// Method to logout a customer
	
	@Override
	public SessionDTO logoutCustomer(SessionDTO sessionToken) {
		
		String token = sessionToken.getToken();
		
		UserSession session = tokenValidationUtil.validateCustomerToken(token);
		
		sessionRepository.delete(session);
		
		sessionToken.setMessage("Logged out sucessfully.");
		
		return sessionToken;
	}
	
	
	
	// Method to check status of session token
	
	
	@Override
	public void checkTokenStatus(String token) {
		tokenValidationUtil.validateTokenAndGetSession(token);
	}

	
	// Method to login a valid seller and generate a seller token
	
	@Override
	public UserSession loginSeller(SellerDTO seller) {
		
		Seller existingSeller = sellerRepository.findByMobile(seller.getMobile())
				.orElseThrow(() -> new SellerNotFoundException("Seller record does not exist with given mobile number"));
		
		Optional<UserSession> opt = sessionRepository.findByUserId(existingSeller.getSellerId());
		
		if(opt.isPresent()) {
			
			UserSession user = opt.get();
			
			if(user.getSessionEndTime().isBefore(LocalDateTime.now())) {
				sessionRepository.delete(user);	
			}
			else
				throw new LoginException("User already logged in");
			
		}
		
		
		if(passwordEncoderUtil.matchesPassword(seller.getPassword(), existingSeller.getPassword())) {
		
			UserSession newSession = new UserSession();
			
			newSession.setUserId(existingSeller.getSellerId());
			newSession.setUserType("seller");
			newSession.setSessionStartTime(LocalDateTime.now());
			newSession.setSessionEndTime(LocalDateTime.now().plusHours(TokenValidationUtil.SESSION_DURATION_HOURS));
			
			UUID uuid = UUID.randomUUID();
			String token = TokenValidationUtil.SELLER_PREFIX + uuid.toString().split("-")[0];
			
			newSession.setToken(token);
			
			return sessionRepository.save(newSession);
		}
		else {
			throw new LoginException("Password Incorrect. Try again.");
		}
	}

	
	// Method to logout a seller and delete his session token
	
	@Override
	public SessionDTO logoutSeller(SessionDTO session) {
		
		String token = session.getToken();
		
		UserSession user = tokenValidationUtil.validateSellerToken(token);
		
		sessionRepository.delete(user);
		
		session.setMessage("Logged out sucessfully.");
		
		return session;
	}
	
	
	@Override
	public void deleteExpiredTokens() {
		
		List<UserSession> users = sessionRepository.findAll();
		
		if(users.size() > 0) {
			for(UserSession user:users) {
				LocalDateTime endTime = user.getSessionEndTime();
				if(endTime.isBefore(LocalDateTime.now())) {
					sessionRepository.delete(user);
				}
			}
		}
	}
	
}
