package com.masai.dto;

import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.CreditCardNumber;

import com.masai.models.CreditCard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class OrderDTO {
	
	@NotNull
	private CreditCard cardNumber;
	@NotNull
	private String addressType;
}
