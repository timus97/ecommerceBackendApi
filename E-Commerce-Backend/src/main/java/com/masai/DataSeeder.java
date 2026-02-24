package com.masai;

import com.masai.models.*;
import com.masai.repository.CustomerDao;
import com.masai.repository.ProductDao;
import com.masai.repository.SellerDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * DataSeeder – active ONLY when the "test" Spring profile is enabled.
 *
 * Inserts a rich, self-consistent set of dummy data so that every API
 * endpoint can be exercised immediately after startup without manual setup.
 *
 * Seeded entities
 * ───────────────
 *  Sellers  : 2  (seller1 / seller2)
 *  Customers: 3  (customer1 / customer2 / customer3)
 *  Products : 10 (spread across all 5 categories, various statuses)
 *  Wishlists: auto-created per customer during registration
 *
 * All passwords follow the app validation rule: 8-15 chars, A-Za-z0-9!@#$%^&*_
 * All mobile numbers follow: starts with 6/7/8/9, 10 digits total
 */
@Component
@Profile("test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired private SellerDao sellerDao;
    @Autowired private CustomerDao customerDao;
    @Autowired private ProductDao productDao;

    @Override
    public void run(String... args) {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  DataSeeder: populating test database …              ║");
        log.info("╚══════════════════════════════════════════════════════╝");

        // ── 1. SELLERS ────────────────────────────────────────────────────────
        Seller seller1 = new Seller();
        seller1.setFirstName("Ravi");
        seller1.setLastName("Sharma");
        seller1.setMobile("9876543210");
        seller1.setEmailId("ravi.sharma@techstore.com");
        seller1.setPassword("Seller@123");
        seller1.setProduct(new ArrayList<>());
        sellerDao.save(seller1);

        Seller seller2 = new Seller();
        seller2.setFirstName("Priya");
        seller2.setLastName("Nair");
        seller2.setMobile("8765432109");
        seller2.setEmailId("priya.nair@fashionhub.com");
        seller2.setPassword("Seller@456");
        seller2.setProduct(new ArrayList<>());
        sellerDao.save(seller2);

        log.info("✔  Sellers seeded: {} and {}", seller1.getEmailId(), seller2.getEmailId());

        // ── 2. PRODUCTS ───────────────────────────────────────────────────────
        // Electronics (seller1)
        Product p1 = buildProduct("Samsung Galaxy S23", 74999.0,
                "Flagship Android smartphone with 200MP camera", "Samsung",
                50, CategoryEnum.ELECTRONICS, ProductStatus.AVAILABLE, seller1);

        Product p2 = buildProduct("Sony WH-1000XM5", 29999.0,
                "Industry-leading noise cancelling wireless headphones", "Sony",
                30, CategoryEnum.ELECTRONICS, ProductStatus.AVAILABLE, seller1);

        Product p3 = buildProduct("Apple MacBook Air M2", 114900.0,
                "Supercharged by the next-generation M2 chip", "Apple",
                0, CategoryEnum.ELECTRONICS, ProductStatus.OUTOFSTOCK, seller1);

        // Fashion (seller2)
        Product p4 = buildProduct("Levi's 511 Slim Jeans", 3499.0,
                "Classic slim-fit jeans in stretch denim", "Levis",
                200, CategoryEnum.FASHION, ProductStatus.AVAILABLE, seller2);

        Product p5 = buildProduct("Nike Air Max 270", 12995.0,
                "Lifestyle shoe with large Air unit for all-day comfort", "Nike",
                80, CategoryEnum.FASHION, ProductStatus.AVAILABLE, seller2);

        // Books (seller1)
        Product p6 = buildProduct("Clean Code", 699.0,
                "A handbook of agile software craftsmanship by Robert C. Martin", "Pearson",
                150, CategoryEnum.BOOKS, ProductStatus.AVAILABLE, seller1);

        Product p7 = buildProduct("Effective Java 3rd Edition", 849.0,
                "Best practices for the Java platform by Joshua Bloch", "Addison-Wesley",
                120, CategoryEnum.BOOKS, ProductStatus.AVAILABLE, seller1);

        // Furniture (seller2)
        Product p8 = buildProduct("Ergonomic Office Chair", 15999.0,
                "Lumbar support mesh chair for long work sessions", "DeckUp",
                25, CategoryEnum.FURNITURE, ProductStatus.AVAILABLE, seller2);

        // Groceries (seller2)
        Product p9 = buildProduct("Organic Green Tea 100g", 299.0,
                "Premium Darjeeling first flush organic green tea", "Organic India",
                500, CategoryEnum.GROCERIES, ProductStatus.AVAILABLE, seller2);

        Product p10 = buildProduct("Basmati Rice 5kg", 649.0,
                "Extra-long grain aged basmati rice", "India Gate",
                300, CategoryEnum.GROCERIES, ProductStatus.AVAILABLE, seller2);

        productDao.save(p1); productDao.save(p2); productDao.save(p3);
        productDao.save(p4); productDao.save(p5);
        productDao.save(p6); productDao.save(p7);
        productDao.save(p8);
        productDao.save(p9); productDao.save(p10);

        log.info("✔  Products seeded: 10 products across 5 categories");

        // ── 3. CUSTOMERS ──────────────────────────────────────────────────────
        // Customer 1 – fully set up with address + credit card
        Customer c1 = buildCustomer("Amit", "Verma",
                "9123456780", "amit.verma@gmail.com", "Amit@1234");
        setupCart(c1);
        setupWishlist(c1);
        setupAddress(c1, "home",
                "12", "Sunrise Apartments", "Koramangala",
                "Bangalore", "KARNATAKA", "560034");
        setupCreditCard(c1, "4111111111111111", "12/26", "123");
        customerDao.save(c1);

        // Customer 2 – has address, no credit card (to test PENDING order flow)
        Customer c2 = buildCustomer("Sneha", "Patel",
                "9234567801", "sneha.patel@gmail.com", "Sneha@5678");
        setupCart(c2);
        setupWishlist(c2);
        setupAddress(c2, "home",
                "45", "Green Valley", "Bandra West",
                "Mumbai", "MAHARASHTRA", "400050");
        setupAddress(c2, "work",
                "101", "Tech Park", "Whitefield",
                "Bangalore", "KARNATAKA", "560066");
        customerDao.save(c2);

        // Customer 3 – minimal setup for registration / login testing
        Customer c3 = buildCustomer("Rahul", "Singh",
                "9345678012", "rahul.singh@gmail.com", "Rahul@9012");
        setupCart(c3);
        setupWishlist(c3);
        customerDao.save(c3);

        log.info("✔  Customers seeded:");
        log.info("     Customer 1: mobile=9123456780  password=Amit@1234  (address + card ready)");
        log.info("     Customer 2: mobile=9234567801  password=Sneha@5678 (2 addresses, no card)");
        log.info("     Customer 3: mobile=9345678012  password=Rahul@9012 (minimal)");
        log.info("   Sellers:");
        log.info("     Seller 1:   mobile=9876543210  password=Seller@123");
        log.info("     Seller 2:   mobile=8765432109  password=Seller@456");
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  DataSeeder: DONE – app ready for API testing        ║");
        log.info("╚══════════════════════════════════════════════════════╝");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product buildProduct(String name, Double price, String desc,
                                  String manufacturer, Integer qty,
                                  CategoryEnum category, ProductStatus status,
                                  Seller seller) {
        Product p = new Product();
        p.setProductName(name);
        p.setPrice(price);
        p.setDescription(desc);
        p.setManufacturer(manufacturer);
        p.setQuantity(qty);
        p.setCategory(category);
        p.setStatus(status);
        p.setSeller(seller);
        p.setAverageRating(0.0);
        p.setReviewCount(0L);
        p.setReviews(new ArrayList<>());
        return p;
    }

    private Customer buildCustomer(String first, String last,
                                    String mobile, String email, String password) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setMobileNo(mobile);
        c.setEmailId(email);
        c.setPassword(password);
        c.setCreatedOn(LocalDateTime.now());
        c.setOrders(new ArrayList<>());
        return c;
    }

    private void setupCart(Customer c) {
        Cart cart = new Cart();
        cart.setCartItems(new ArrayList<>());
        cart.setCartTotal(0.0);
        c.setCustomerCart(cart);
    }

    private void setupWishlist(Customer c) {
        Wishlist w = new Wishlist();
        w.setCustomer(c);
        w.setWishlistItems(new ArrayList<>());
        c.setCustomerWishlist(w);
    }

    private void setupAddress(Customer c, String type,
                               String streetNo, String building, String locality,
                               String city, String state, String pincode) {
        Address addr = new Address();
        addr.setStreetNo(streetNo);
        addr.setBuildingName(building);
        addr.setLocality(locality);
        addr.setCity(city);
        addr.setState(state);
        addr.setPincode(pincode);
        c.getAddress().put(type, addr);
    }

    private void setupCreditCard(Customer c, String number, String validity, String cvv) {
        CreditCard card = new CreditCard(number, validity, cvv);
        c.setCreditCard(card);
    }
}
