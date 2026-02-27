package com.masai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.masai.models.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
}
