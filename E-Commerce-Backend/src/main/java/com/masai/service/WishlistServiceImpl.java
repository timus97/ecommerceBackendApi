package com.masai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.masai.exception.CustomerNotFoundException;
import com.masai.exception.LoginException;
import com.masai.exception.ProductNotFoundException;
import com.masai.exception.WishlistException;
import com.masai.models.Cart;
import com.masai.dto.CartDTO;
import com.masai.models.Customer;
import com.masai.models.Product;
import com.masai.models.UserSession;
import com.masai.models.Wishlist;
import com.masai.models.WishlistItem;
import com.masai.dto.WishlistResponseDTO;
import com.masai.repository.CartDao;
import com.masai.repository.CustomerDao;
import com.masai.repository.ProductDao;
import com.masai.repository.SessionDao;
import com.masai.repository.WishlistDao;
import com.masai.repository.WishlistItemDao;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private WishlistDao wishlistDao;

    @Autowired
    private WishlistItemDao wishlistItemDao;

    @Autowired
    private CartDao cartDao;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private LoginLogoutService loginService;

    // -----------------------------------------------------------------------
    // Helper: validate customer token and return the Customer entity.
    // Also lazily creates a Wishlist for customers who registered before the
    // feature was introduced (so existing accounts always have a wishlist).
    // -----------------------------------------------------------------------
    private Customer resolveCustomer(String token) {
        if (!token.contains("customer")) {
            throw new LoginException("Invalid session token for customer");
        }
        loginService.checkTokenStatus(token);

        UserSession user = sessionDao.findByToken(token)
                .orElseThrow(() -> new LoginException("Session not found. Please login again."));

        Customer customer = customerDao.findById(user.getUserId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        // Lazily initialise wishlist for existing customers
        if (customer.getCustomerWishlist() == null) {
            Wishlist wishlist = new Wishlist();
            wishlist.setCustomer(customer);
            wishlist = wishlistDao.save(wishlist);
            customer.setCustomerWishlist(wishlist);
            customerDao.save(customer);
        }

        return customer;
    }

    // -----------------------------------------------------------------------
    // Helper: map a WishlistItem to its response DTO
    // -----------------------------------------------------------------------
    private WishlistResponseDTO toDTO(WishlistItem item) {
        Product p = item.getProduct();
        return new WishlistResponseDTO(
                item.getWishlistItemId(),
                p.getProductId(),
                p.getProductName(),
                p.getPrice(),
                p.getDescription(),
                p.getManufacturer(),
                p.getCategory(),
                p.getStatus(),
                p.getAverageRating(),
                p.getReviewCount(),
                item.getAddedAt()
        );
    }

    // -----------------------------------------------------------------------
    // 1. Add to wishlist
    // -----------------------------------------------------------------------
    @Override
    @Transactional
    public WishlistResponseDTO addToWishlist(Integer productId, String token) {
        Customer customer = resolveCustomer(token);
        Wishlist wishlist = customer.getCustomerWishlist();

        // Duplicate check
        Optional<WishlistItem> existing = wishlistItemDao
                .findByWishlist_WishlistIdAndProduct_ProductId(wishlist.getWishlistId(), productId);
        if (existing.isPresent()) {
            throw new WishlistException("Product is already in your wishlist");
        }

        Product product = productDao.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        WishlistItem item = new WishlistItem();
        item.setProduct(product);
        item.setAddedAt(LocalDateTime.now());
        item.setWishlist(wishlist);

        WishlistItem saved = wishlistItemDao.save(item);
        return toDTO(saved);
    }

    // -----------------------------------------------------------------------
    // 2. View wishlist (sorted by most recently added)
    // -----------------------------------------------------------------------
    @Override
    public List<WishlistResponseDTO> getWishlist(String token) {
        Customer customer = resolveCustomer(token);
        Wishlist wishlist = customer.getCustomerWishlist();

        return wishlistItemDao
                .findByWishlist_WishlistIdOrderByAddedAtDesc(wishlist.getWishlistId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // 3. Remove from wishlist
    // -----------------------------------------------------------------------
    @Override
    @Transactional
    public String removeFromWishlist(Integer productId, String token) {
        Customer customer = resolveCustomer(token);
        Wishlist wishlist = customer.getCustomerWishlist();

        WishlistItem item = wishlistItemDao
                .findByWishlist_WishlistIdAndProduct_ProductId(wishlist.getWishlistId(), productId)
                .orElseThrow(() -> new WishlistException("Product with id " + productId + " is not in your wishlist"));

        wishlistItemDao.delete(item);
        return "Product removed from wishlist successfully";
    }

    // -----------------------------------------------------------------------
    // 4. Move to cart (atomic: remove from wishlist + add to cart)
    // -----------------------------------------------------------------------
    @Override
    @Transactional
    public Cart moveToCart(Integer productId, String token) {
        Customer customer = resolveCustomer(token);
        Wishlist wishlist = customer.getCustomerWishlist();

        // Ensure the item is actually in the wishlist
        WishlistItem item = wishlistItemDao
                .findByWishlist_WishlistIdAndProduct_ProductId(wishlist.getWishlistId(), productId)
                .orElseThrow(() -> new WishlistException("Product with id " + productId + " is not in your wishlist"));

        // Remove from wishlist
        wishlistItemDao.delete(item);

        // Add to cart — reuse existing CartItemService logic
        Cart customerCart = customer.getCustomerCart();
        CartDTO cartDTO = new CartDTO(productId, null, null, 1);

        com.masai.models.CartItem cartItem = cartItemService.createItemforCart(cartDTO);

        List<com.masai.models.CartItem> cartItems = customerCart.getCartItems();

        boolean alreadyInCart = false;
        for (com.masai.models.CartItem c : cartItems) {
            if (c.getCartProduct().getProductId().equals(productId)) {
                c.setCartItemQuantity(c.getCartItemQuantity() + 1);
                customerCart.setCartTotal(customerCart.getCartTotal() + c.getCartProduct().getPrice());
                alreadyInCart = true;
                break;
            }
        }

        if (!alreadyInCart) {
            cartItems.add(cartItem);
            double currentTotal = customerCart.getCartTotal() == null ? 0.0 : customerCart.getCartTotal();
            customerCart.setCartTotal(currentTotal + cartItem.getCartProduct().getPrice());
        }

        return cartDao.save(customerCart);
    }

    // -----------------------------------------------------------------------
    // 5. Check if wishlisted
    // -----------------------------------------------------------------------
    @Override
    public boolean isWishlisted(Integer productId, String token) {
        Customer customer = resolveCustomer(token);
        Wishlist wishlist = customer.getCustomerWishlist();

        return wishlistItemDao
                .findByWishlist_WishlistIdAndProduct_ProductId(wishlist.getWishlistId(), productId)
                .isPresent();
    }
}
