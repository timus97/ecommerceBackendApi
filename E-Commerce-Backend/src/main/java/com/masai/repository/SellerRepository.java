package com.masai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.masai.models.Seller;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
	
	Optional<Seller> findByMobile(String mobile);
	
}
