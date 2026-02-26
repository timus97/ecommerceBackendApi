package com.masai.models;


import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seller {
	@Id
	@GeneratedValue
	private Integer sellerId;
	
	@NotNull(message="Please enter the first name")
	@Pattern(regexp="[A-Za-z\\s]+", message="First Name should contains alphabets only")
	private String firstName;
	
	@NotNull(message="Please enter the last name")
	@Pattern(regexp="[A-Za-z\\s]+", message="First Name should contains alphabets only")
	private String lastName;
	
   @Pattern(regexp="([A-Za-z0-9!@#$%^&*_]{8,15}|\\$2[aby]\\$\\d{2}\\$.{53})", message="Please Enter a valid Password")
	private String password;
	
	@NotNull(message="Please enter your mobile Number")
	@Pattern(regexp="[6789]{1}[0-9]{9}", message="Enter a valid Mobile Number")
	@Column(unique = true)
	private String mobile;
	
	
	@Email
	@Column(unique = true)
	private String emailId;
	

	@OneToMany
	@JsonIgnore
	private List<Product> product;
	

}
