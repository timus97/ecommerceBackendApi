package com.masai.models;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a single product saved in a customer's wishlist.
 * Tracks when the product was added so the list can be sorted by most recently added.
 */
@Entity
@Table(name = "wishlist_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer wishlistItemId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @ManyToOne
    @JoinColumn(name = "wishlist_id", nullable = false)
    @JsonIgnore
    private Wishlist wishlist;
}
