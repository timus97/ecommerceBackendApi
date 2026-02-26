package com.masai.models;

import jakarta.persistence.Embedded;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.CreditCardNumber;

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
	@Embedded
	private CreditCard cardNumber;
	@NotNull
	private String addressType;
}
