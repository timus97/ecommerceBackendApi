package com.masai.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
public class CartItem {
	
	@Id
	@GeneratedValue
	private Integer cartItemId;
	
	
	@OneToOne
	@JsonIgnoreProperties(value={
			"productId",
			"seller",
			"quantity"
			
	})
	private Product cartProduct;
	
	private Integer cartItemQuantity;
	
}
