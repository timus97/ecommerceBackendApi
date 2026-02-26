package com.masai.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class SellerDTO {
	
	
	@NotNull(message="Please enter your mobile Number")
	@Pattern(regexp="[6789]{1}[0-9]{9}", message="Enter a valid Mobile Number")
	private String mobile;
	
	
	@Pattern(regexp="[A-Za-z0-9!@#$%^&*_]{8,15}", message="Please Enter a valid Password")
	private String password;
	
}
