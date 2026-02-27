package com.masai.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.masai.models.Customer;
import com.masai.models.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
	public List<Order> findByDate(LocalDate date);
	
	
	@Query("select c from Customer c where c.customerId = customerId")
	public Customer getCustomerByOrderid(@Param("customerId") Integer customerId);
	
}
