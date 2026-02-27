# E-Commerce Backend – Refactoring Opportunities

This document consolidates **all refactoring opportunities** in the repo: the existing [REFACTORING_GUIDE.md](REFACTORING_GUIDE.md) plus **additional findings** from a full codebase review. Items are grouped by area with concrete locations and suggested changes.

---

## Summary: What’s Already Done vs Pending

| Area | Status |
|------|--------|
| Token validation centralization | ✅ Done – `TokenValidationUtil` with `validateCustomerToken` / `validateSellerToken` used in services |
| OrderController injecting OrderDao | ✅ Not present – controller uses only `OrderService` |
| OrderServiceImpl depending on CartService | ✅ Done – uses `CartService` interface |
| CategoryNotFoundException → 404 | ✅ Fixed – returns `BAD_REQUEST` in `GlobalExceptionHandler` |
| Duplicate DTOs (models vs dto) | ⚠️ Pending – both packages still have DTOs; no imports from `models.*DTO` found (code uses `dto`), but `models` DTOs are dead code |
| Optional `.get()` without check | ⚠️ Pending – multiple services still use `.get()` |
| Duplicate *Dao vs *Repository interfaces | ⚠️ **New** – all `*Dao` interfaces are unused; code uses only `*Repository` |
| Constructor vs field injection | ⚠️ Pending – all controllers/services use `@Autowired` on fields |
| Validation error response (single vs all errors) | ⚠️ Pending – `getFieldError()` can be null → NPE risk; only first error returned |
| Duplicate seller registration | ⚠️ Pending – `POST /addseller` and `POST /register/seller` both exist |
| JaCoCo in pom.xml | ⚠️ **New** – JaCoCo added as dependency; should be plugin only |
| Response types in OrderController | ⚠️ Pending – some handlers return raw `List<Order>` / `Order` instead of `ResponseEntity<>` |

---

## 1. Package & DTO / Model Organization

### 1.1 Remove duplicate DTOs in `models` (dead code)

**Location:** `src/main/java/com/masai/models/`  
**Issue:** DTOs exist in both `com.masai.models` and `com.masai.dto`. No code imports from `models.*DTO`; all use `com.masai.dto`. The following in `models` are therefore dead code:

- `SellerDTO`, `CustomerDTO`, `SessionDTO`, `OrderDTO`, `ProductDTO`, `CartDTO`
- `CustomerUpdateDTO`, `ReviewRequestDTO`, `ReviewResponseDTO`, `ReviewSummaryDTO`, `WishlistResponseDTO`, `ProductSearchResponseDTO`, `ProductSearchFilterDTO`

**Refactor:**

1. Keep only `com.masai.dto` as the single source for DTOs.
2. Delete the duplicate DTO classes from `com.masai.models` (after confirming no references).
3. Convention: `models` = JPA entities only; `dto` = API request/response and internal transfer objects.

---

## 2. Repositories: Remove duplicate *Dao interfaces (dead code)

**Location:** `src/main/java/com/masai/repository/`  
**Issue:** For each entity there are two interfaces with the same role:

- `*Repository` (e.g. `ProductRepository`, `SessionRepository`) – **used** everywhere.
- `*Dao` (e.g. `ProductDao`, `SessionDao`) – **never imported**; same methods as the corresponding Repository.

So `ProductDao`, `OrderDao`, `CustomerDao`, `SellerDao`, `SessionDao`, `CartDao`, `CartItemDao`, `AddressDao`, `ReviewDao`, `WishlistDao`, `WishlistItemDao` are all dead code.

**Refactor:**

1. Delete all `*Dao.java` interfaces.
2. Keep and use only `*Repository` interfaces. No need to rename; just remove duplication.

---

## 3. Optional handling: Replace `.get()` with safe patterns

**Issue:** Using `Optional.get()` without checking can throw `NoSuchElementException`. Prefer `orElseThrow(...)`, `orElse()`, or `ifPresent()`.

**Locations and changes:**

| File | Line(s) | Current | Refactor |
|------|--------|---------|----------|
| `TokenValidationUtil.java` | 39 | `session.get().getSessionEndTime()` | Use `session.map(UserSession::getSessionEndTime).filter(...)` or `session.filter(s -> !s.getSessionEndTime().isBefore(now)).isPresent()` without `.get()`. |
| `SellerServiceImpl.java` | 70, 96, 169 | `seller.get()`, `opt.get()` | Use `orElseThrow(() -> new SellerException("..."))`. |
| `OrderServiceImpl.java` | 206 | `opt.get()` | Use `orElseThrow(() -> new OrderException("..."))`. |
| `ProductServiceImpl.java` | 51, 74, 87, 102, 149 | `opt.get()` | Use `orElseThrow(() -> new ProductNotFoundException("..."))`. Note: line 102 is a no-op `opt.get();` – remove or use result. |
| `LoginLogoutServiceImpl.java` | 59, 131 | `opt.get()` | Use `orElseThrow` or handle empty explicitly (e.g. create new session). |
| `CustomerServiceImpl.java` | 135, 137 | `opt.get()`, `res.get()` | After `if(opt.isPresent()) ... else ... res.get()` – use `opt.orElse(res.orElseThrow(...))` or two `orElseThrow` paths. |
| `CartServiceImpl.java` | 95 | `opt.get()` | Use `customerRepository.findById(...).orElseThrow(() -> new CustomerNotFoundException("..."))` (same as in other methods in that class). |

---

## 4. Exception handling & HTTP status

### 4.1 Validation → 400 and avoid NPE

**Location:** `GlobalExceptionHandler.methodArgumentNotValidExceptionHandler`  
**Issue:**

- `manv.getBindingResult().getFieldError()` can be **null** → NPE.
- Only the first field error is returned; clients see only one validation message.

**Refactor:**

1. Use `getFieldErrors()` and build a list of `{ field, message }` (e.g. new `ValidationErrorDetails` or reuse `ErrorDetails` with a list of messages).
2. Return status `HttpStatus.BAD_REQUEST` (400) and the full list of field errors in the response body.

### 4.2 Generic Exception → 500

**Location:** `GlobalExceptionHandler.exceptionHandler(Exception e, ...)`  
**Issue:** Returns `HttpStatus.BAD_REQUEST`. Unhandled server errors should be 500.

**Refactor:** Map generic `Exception` to `HttpStatus.INTERNAL_SERVER_ERROR` (500). Optionally log the exception and return a generic message to the client.

---

## 5. Controllers

### 5.1 Consistent response types (OrderController)

**Location:** `OrderController.java`  
**Issue:** Some handlers return `ResponseEntity<Order>`, others return raw `List<Order>`, `Order`, or `Customer`. This makes status codes and headers inconsistent.

**Refactor:**

- Prefer **always** returning `ResponseEntity<...>`.
- Use explicit status: list/single resource → `OK` or `NOT_FOUND`, create → `CREATED`, update → `OK`/`ACCEPTED`, delete → `OK` or `NO_CONTENT`.
- Example: `getAllOrders()` → `return ResponseEntity.ok(oService.getAllOrders());`, and similarly for `getOrdersByOrderId`, `cancelTheOrderByOrderId`, `getCustomerDetailsByOrderId`.

### 5.2 Duplicate seller registration endpoints

**Location:**  
- `LoginController`: `POST /register/seller`  
- `SellerController`: `POST /addseller`  

Both call `sellerService.addSeller(seller)`.

**Refactor:**

- Keep one public endpoint (e.g. `POST /register/seller`).
- Deprecate or remove `POST /addseller`; if backward compatibility is required, make it delegate to the same logic and mark `@Deprecated` with a note to use `/register/seller`.

### 5.3 Constructor injection instead of field injection

**Location:** All controllers and services (see `@Autowired` grep).  
**Refactor:** Use constructor injection for required dependencies (single constructor → no `@Autowired` needed on constructor in Spring 4.3+). Improves testability and makes dependencies explicit.

---

## 6. Services

### 6.1 Naming consistency

**Location:** Various services.  
**Issue:** Short or unclear names: `cs`, `oService`, `sService`, `odto`, `prod`, `opt`, `res`.

**Refactor:** Use clear names: `customerService`, `orderService`, `sellerService`, `orderDto`, `product`, `optionalSeller`, `existingCustomer`, etc.

### 6.2 Extract “resolve current user” helpers

**Location:** `CustomerServiceImpl`, `SellerServiceImpl`.  
**Issue:** Repeated pattern: validate token → get session → load customer/seller by `user.getUserId()`.

**Refactor:**

- In `CustomerServiceImpl`: private `getCustomerFromToken(String token)` that returns `Customer` (validate token + load customer; throw if not found).
- In `SellerServiceImpl`: private `getSellerFromToken(String token)` that returns `Seller`.
- Use these at the start of each method that needs “current customer” or “current seller” to remove duplication.

### 6.3 ProductServiceImpl: redundant code and Optional misuse

**Location:** `ProductServiceImpl.java`  
**Issue:**

- Line 102: `opt.get();` does nothing; then `productRepository.save(prod)` uses `prod` from the same `opt`. Use `opt.orElseThrow(...)` and one variable.
- `if(opt!=null)` is always true for an `Optional`; use `opt.isPresent()` and `orElseThrow` for the empty case (lines 145–157).
- Redundant semicolons (e.g. after `save(product);`).

**Refactor:** Use `Optional.orElseThrow` consistently; remove no-op `opt.get()` and redundant `if(opt!=null)`; clean up semicolons.

---

## 7. Configuration & build

### 7.1 JaCoCo in pom.xml

**Location:** `E-Commerce-Backend/pom.xml` (dependencies section).  
**Issue:** JaCoCo is added as a **dependency** with artifact `jacoco-maven-plugin`. Maven plugins belong under `<build><plugins>`, not `<dependencies>`. The same plugin is already correctly declared in `<build><plugins>`.

**Refactor:** Remove the JaCoCo **dependency** block (lines ~112–117). Keep only the JaCoCo **plugin** under `<build><plugins>`.

### 7.2 Session duration and date format

**Location:** `LoginLogoutServiceImpl`, `OrderController`.  
**Issue:** Session duration (e.g. 1 hour) and date pattern (`dd-MM-yyyy`) are hardcoded.

**Refactor:** Move to configuration (e.g. `app.session.duration-hours=1`, `app.date.format=dd-MM-yyyy`) and inject via `@Value` or `@ConfigurationProperties`.

---

## 8. Entities & API contract (medium/long term)

### 8.1 Response/request DTOs instead of entities

**Issue:** Many endpoints return or accept JPA entities (`Order`, `Product`, `Customer`, etc.) directly, which can leak internals and tie the API to the persistence model.

**Refactor:** Introduce response DTOs for reads and request DTOs for create/update; map in the service layer and return DTOs from controllers. See REFACTORING_GUIDE.md sections 7.1 and 7.2.

---

## 9. Test script (test_api.sh)

**Location:** `test_api.sh` (root).  
**Observation:** Script is long (~999 lines) but already uses helpers (`assert`, `assert_any`, `curl_json`, `section`, `extract`). No critical duplication found; optional improvements:

- Extract repeated “login as customer/seller and capture token” into small functions if the same block appears many times.
- Consider splitting into smaller files (e.g. `test_api_helpers.sh` + scenario-specific scripts) if it grows further.

---

## Suggested order of work

| Priority | Task | Effort | Impact |
|----------|------|--------|--------|
| 1 | Remove duplicate DTOs from `models` (1.1) | Low | Less confusion, no dead code |
| 2 | Remove duplicate *Dao interfaces (Section 2) | Low | Single repository abstraction |
| 3 | Fix JaCoCo dependency in pom.xml (7.1) | Low | Correct build model |
| 4 | Replace Optional `.get()` with orElseThrow / safe patterns (Section 3) | Medium | Fewer runtime exceptions |
| 5 | Fix validation handler NPE and return all field errors (4.1) | Low | Correct and robust validation API |
| 6 | Generic Exception → 500 (4.2) | Low | Correct HTTP semantics |
| 7 | OrderController consistent ResponseEntity (5.1) | Low | Consistent API contract |
| 8 | Single seller registration endpoint (5.2) | Low | Clear API surface |
| 9 | Constructor injection + naming (5.3, 6.1) | Medium | Testability and readability |
| 10 | getCustomerFromToken / getSellerFromToken helpers (6.2) | Low | DRY and clarity |
| 11 | ProductServiceImpl Optional and redundant code (6.3) | Low | Correctness and clarity |
| 12 | Session/date config (7.2) | Low | Flexibility |
| 13 | Response/request DTOs for API (8.1) | High | Stable, clear API |

You can tackle these in small PRs: e.g. first dead code (DTOs + Daos) and build (JaCoCo), then Optional and exception handling, then controllers and services.
