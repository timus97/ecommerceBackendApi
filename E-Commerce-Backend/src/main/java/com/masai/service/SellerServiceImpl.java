package com.masai.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.exception.LoginException;
import com.masai.exception.SellerException;
import com.masai.models.Seller;
import com.masai.dto.SellerDTO;
import com.masai.dto.SessionDTO;
import com.masai.models.UserSession;
import com.masai.repository.SellerRepository;
import com.masai.util.PasswordEncoderUtil;
import com.masai.util.TokenValidationUtil;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerServiceImpl implements SellerService {
	
	@Autowired
	private SellerRepository sellerRepository;
	
	@Autowired
	private LoginLogoutService loginService;
	
	@Autowired
	private TokenValidationUtil tokenValidationUtil;
	
	@Autowired
	private PasswordEncoderUtil passwordEncoderUtil;
	

	@Override
	@Transactional
	public Seller addSeller(Seller seller) {
		// Reject duplicate mobile
		if (sellerRepository.findByMobile(seller.getMobile()).isPresent()) {
			throw new SellerException("Seller already exists with this mobile number. Please try to login.");
		}
		// Hash the password using bcrypt
		String hashedPassword = passwordEncoderUtil.encodePassword(seller.getPassword());
		seller.setPassword(hashedPassword);
		
		Seller add= sellerRepository.save(seller);
		
		return add;
	}

	@Override
	public List<Seller> getAllSellers() throws SellerException {
		
		List<Seller> sellers= sellerRepository.findAll();
		
		if(sellers.size()>0) {
			return sellers;
		}
		else throw new SellerException("No Seller Found !");
		
	}

	@Override
	public Seller getSellerById(Integer sellerId) {
		
		Optional<Seller> seller=sellerRepository.findById(sellerId);
		
		if(seller.isPresent()) {
			return seller.get();
		}
		else throw new SellerException("Seller not found for this ID: "+sellerId);
	}

	@Override
	@Transactional
	public Seller updateSeller(Seller seller, String token) {
		
		tokenValidationUtil.validateSellerToken(token);
		
		Seller existingSeller=sellerRepository.findById(seller.getSellerId()).orElseThrow(()-> new SellerException("Seller not found for this Id: "+seller.getSellerId()));
		Seller newSeller= sellerRepository.save(seller);
		return newSeller;
	}

	@Override
	@Transactional
	public Seller deleteSellerById(Integer sellerId, String token) {
		
		UserSession user = tokenValidationUtil.validateSellerToken(token);
		
		Optional<Seller> opt=sellerRepository.findById(sellerId);
		
		if(opt.isPresent()) {
			
			Seller existingseller=opt.get();
			
			if(user.getUserId() == existingseller.getSellerId()) {
				sellerRepository.delete(existingseller);
				
				// logic to log out a seller after he deletes his account
				SessionDTO session = new SessionDTO();
				session.setToken(token);
				loginService.logoutSeller(session);
				
				return existingseller;
			}
			else {
				throw new SellerException("Verification Error in deleting seller account");
			}
		}
		else throw new SellerException("Seller not found for this ID: "+sellerId);
		
	}

	@Override
	@Transactional
	public Seller updateSellerMobile(SellerDTO sellerdto, String token) throws SellerException {
		
		UserSession user = tokenValidationUtil.validateSellerToken(token);
		
		Seller existingSeller=sellerRepository.findById(user.getUserId()).orElseThrow(()->new SellerException("Seller not found for this ID: "+ user.getUserId()));
		
		if(passwordEncoderUtil.matchesPassword(sellerdto.getPassword(), existingSeller.getPassword())) {
			existingSeller.setMobile(sellerdto.getMobile());
			return sellerRepository.save(existingSeller);
		}
		else {
			throw new SellerException("Error occured in updating mobile. Please enter correct password");
		}
		
	}

	@Override
	public Seller getSellerByMobile(String mobile, String token) throws SellerException {
		
		tokenValidationUtil.validateSellerToken(token);
		
		Seller existingSeller = sellerRepository.findByMobile(mobile).orElseThrow( () -> new SellerException("Seller not found with given mobile"));
		
		return existingSeller;
	}
	
	@Override
	public Seller getCurrentlyLoggedInSeller(String token) throws SellerException{
		
		UserSession user = tokenValidationUtil.validateSellerToken(token);
		
		Seller existingSeller=sellerRepository.findById(user.getUserId()).orElseThrow(()->new SellerException("Seller not found for this ID"));
		
		return existingSeller;
		
	}
	
	
	// Method to update password - based on current token
	
	@Override
	@Transactional
	public SessionDTO updateSellerPassword(SellerDTO sellerDTO, String token) {
				
		UserSession user = tokenValidationUtil.validateSellerToken(token);
			
		Optional<Seller> opt = sellerRepository.findById(user.getUserId());
			
		if(opt.isEmpty())
			throw new SellerException("Seller does not exist");
			
		Seller existingSeller = opt.get();
			
			
		if(sellerDTO.getMobile().equals(existingSeller.getMobile()) == false) {
			throw new SellerException("Verification error. Mobile number does not match");
		}
			
		// Hash the password using bcrypt
	String hashedPassword = passwordEncoderUtil.encodePassword(sellerDTO.getPassword());
	existingSeller.setPassword(hashedPassword);
			
		sellerRepository.save(existingSeller);
			
		SessionDTO session = new SessionDTO();
			
		session.setToken(token);
			
		loginService.logoutSeller(session);
			
		session.setMessage("Updated password and logged out. Login again with new password");
			
		return session;

	}

}
