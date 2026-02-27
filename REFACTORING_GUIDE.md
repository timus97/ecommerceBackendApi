# E-Commerce Backend – Refactoring Guide

This document lists **refactoring opportunities** across the codebase, grouped by area, with **what to change** and **how to do it**.

---

## 1. Package & DTO / Model Organization

### 1.1 Duplicate DTOs (models vs dto)

**Issue:** Same DTO names exist in both `com.masai.models` and `com.masai.dto`:

- `SellerDTO`, `CustomerDTO`, `SessionDTO`, `OrderDTO` exist in **both** packages.
- Controllers/services import from `com.masai.dto.*`; the `models` versions are redundant and confusing.

**Refactor:**

1. **Keep a single source of truth** – use only `com.masai.dto` for request/response DTOs.
2. **Remove** `com.masai.models.SellerDTO`, `models.CustomerDTO`, `models.SessionDTO`, `models.OrderDTO` (and any other duplicate DTOs in `models`).
3. **Move** any DTO that is only in `models` (e.g. `OrderDTO` if used from dto package) into `dto` and update imports.
4. **Convention:**  
   - `models` = JPA entities only.  
   - `dto` = API request/response and internal transfer objects.

---

## 2. Token / Session Handling (DRY & Security)

### 2.1 Centralize token validation

**Issue:** Token checks are copy-pasted in many services:

- `token.contains("customer")` / `token.contains("seller")`
- `loginService.checkTokenStatus(token)`
- `sessionDao.findByToken(token).get()` (risk of `NoSuchElementException`)

`TokenValidationUtil` exists but is **not used** by most services.

**Refactor:**

1. **Extend `TokenValidationUtil`** with role-aware methods, e.g.:
   - `validateCustomerToken(String token)` → returns `UserSession` or throws `LoginException`
   - `validateSellerToken(String token)` → same
   - Implement both by: null/empty check → `findByToken` → `checkTokenStatus` (or equivalent) → prefix check (`"customer_"` / `"seller_"`) → return session.
2. **Replace** all inline `token.contains(...)`, `checkTokenStatus`, and `findByToken(...).get()` in:
   - `CustomerServiceImpl`
   - `SellerServiceImpl`
   - `CartServiceImpl`
   - `WishlistServiceImpl`
   - `ReviewServiceImpl`  
   with calls to `TokenValidationUtil.validateCustomerToken(token)` or `validateSellerToken(token)`.
3. **Avoid `.get()` on Optional:** use `orElseThrow(...)` everywhere (and centralize that in the util).

### 2.2 Token format as constants

**Issue:** Strings `"customer"`, `"seller"` and session duration (e.g. `plusHours(1)`) are magic values.

**Refactor:**

- Add constants, e.g. in `TokenValidationUtil` or a `AuthConstants` class:
  - `TOKEN_PREFIX_CUSTOMER = "customer_"`
  - `TOKEN_PREFIX_SELLER = "seller_"`
  - `SESSION_DURATION_HOURS = 1`
- Use these in login/session creation and in validation (e.g. `token.startsWith(TOKEN_PREFIX_CUSTOMER)`).

---

## 3. Controllers

### 3.1 Remove DAO from controller (layering)

**Issue:** `OrderController` injects `OrderDao` but does not use it (dead dependency).

**Refactor:**

- Remove `OrderDao oDao` and its `@Autowired` from `OrderController`.
- Ensure all data access goes through `OrderService` (and, if needed, other services).

### 3.2 Consistent response types and status codes

**Issue:**

- Some handlers return `ResponseEntity<T>`, others return raw `T` or `List<T>` (e.g. `OrderController.getAllOrders()`, `getOrdersByOrderId()`, `cancelTheOrderByOrderId()`).
- `ProductController`: unused import `io.swagger.v3.oas.models.security.SecurityScheme.In`.

**Refactor:**

- Prefer **always** returning `ResponseEntity<...>` from REST handlers for consistent status codes and headers.
- Use `HttpStatus` explicitly (e.g. list → `OK`, single resource → `OK`/`FOUND`/`NOT_FOUND`, create → `CREATED`, delete → `OK`/`NO_CONTENT`).
- Remove unused imports (e.g. `In` in `ProductController`).

### 3.3 Duplicate seller registration endpoints

**Issue:** Two ways to register a seller:

- `POST /register/seller` (LoginController)
- `POST /addseller` (SellerController)

Both call `sellerService.addSeller(seller)`.

**Refactor:**

- **Keep one** registration endpoint (e.g. `POST /register/seller`) and document it as the public API.
- **Deprecate or remove** `POST /addseller`; if you need backward compatibility, make it delegate to the same service and mark `@Deprecated` and document the preferred URL.

### 3.4 Constructor injection instead of field injection

**Issue:** Controllers (and services) use `@Autowired` on fields, which makes testing and required dependencies less explicit.

**Refactor:**

- Prefer **constructor injection** for required dependencies, e.g.:
  - Single constructor with `ProductService`, `OrderService`, etc.
  - Omit `@Autowired` if there is a single constructor (Spring 4.3+).
- Keeps dependencies immutable and testable.

---

## 4. Exception Handling & HTTP Status

### 4.1 Validation → 400 instead of 403

**Issue:** `MethodArgumentNotValidException` is mapped to `HttpStatus.FORBIDDEN` (403). Validation errors (e.g. invalid field format) are usually **client error**, not “forbidden”.

**Refactor:**

- In `GlobalExceptionHandler.methodArgumentNotValidExceptionHandler`, use `HttpStatus.BAD_REQUEST` (400).
- Optionally improve message: collect all field errors from `manv.getBindingResult().getFieldErrors()` and include them in the response body (e.g. list of `{ field, message }`).

### 4.2 CategoryNotFoundException → 400 or 404

**Issue:** `CategoryNotFoundException` returns `HttpStatus.NO_CONTENT` (204). “Not found” is better expressed as 404; 204 is for “success with no body”.

**Refactor:**

- Map `CategoryNotFoundException` to `HttpStatus.NOT_FOUND` (404) and return a body with a clear message (same `ErrorDetails` style).

### 4.3 Richer validation error body

**Issue:** Only the first field error is returned (`getFieldError().getDefaultMessage()`).

**Refactor:**

- Loop over `getBindingResult().getFieldErrors()` and build a list of `{ field, message }` (or reuse a small “validation errors” DTO).
- Set status 400 and return that list in the response body.

---

## 5. Services

### 5.1 Depend on interfaces, not implementations

**Issue:** `OrderServiceImpl` depends on `CartServiceImpl` (concrete class) instead of `CartService` (interface).

**Refactor:**

- In `OrderServiceImpl`, declare the dependency as `CartService` and inject `CartService` (Spring will wire `CartServiceImpl`).
- Ensures dependency inversion and easier mocking in tests.

### 5.2 Optional handling: avoid `.get()` without check

**Issue:** Several places use `sessionDao.findByToken(token).get()` or `opt.get()`. If the Optional is empty, this throws `NoSuchElementException`.

**Refactor:**

- Replace with `sessionDao.findByToken(token).orElseThrow(() -> new LoginException("Invalid or expired session token"))` (or use the centralized `TokenValidationUtil` as above).
- Same idea for any `Optional` used in services: prefer `orElseThrow`, `orElse`, or `ifPresent` instead of raw `.get()`.

### 5.3 Extract “resolve current user” helpers

**Issue:** Same pattern repeated: validate token → get session → load seller/customer by `user.getUserId()`.

**Refactor:**

- In `SellerServiceImpl`: one private method `getSellerFromToken(String token)` that validates seller token and returns `Seller`.
- In `CustomerServiceImpl`: same idea, e.g. `getCustomerFromToken(String token)`.
- Call these at the start of each method that needs “current seller” or “current customer”, and remove duplicated token/session/customer or seller resolution.

---

## 6. Repositories (Naming & Consistency)

### 6.1 Naming: Dao vs Repository

**Issue:** Interfaces are named `*Dao` (e.g. `ProductDao`, `OrderDao`) but extend `JpaRepository` and are used as Spring Data repositories.

**Refactor (optional):**

- Rename to `*Repository` (e.g. `ProductRepository`, `OrderRepository`) for consistency with Spring Data and common Java conventions.
- Update all references (services, controllers if any). Low risk, mostly find-replace and renames.

---

## 7. Entities & API Contract

### 7.1 Exposing entities directly in API

**Issue:** Many endpoints return JPA entities (e.g. `Seller`, `Customer`, `Order`, `Product`) directly. This can:

- Leak internal structure and fields (e.g. ids, relations).
- Cause lazy-loading or serialization issues (e.g. `@JsonIgnore` on relations).
- Tightly couple API contract to persistence model.

**Refactor:**

- Introduce **response DTOs** for read operations (e.g. `SellerResponseDTO`, `CustomerResponseDTO`, `OrderResponseDTO`).
- Map entity → DTO in the service layer (or a small mapper component) and return DTOs from controllers.
- Keep entities for persistence and internal use; use DTOs for all public API request/response bodies.

### 7.2 Request DTOs for write operations

**Issue:** Some endpoints accept full entities (e.g. `Product`, `Seller`) in the request body. This ties the API to the entity schema and can allow clients to set fields that should be server-controlled (e.g. ids, audit fields).

**Refactor:**

- Use **request DTOs** for create/update (e.g. `CreateProductRequest`, `UpdateSellerRequest`) with only the fields the client is allowed to set.
- Map request DTO → entity in the service layer, then save. Reduces over-posting and keeps API stable when entities change.

---

## 8. Configuration & Magic Values

### 8.1 Session duration and token config

**Issue:** Session length (e.g. 1 hour) is hardcoded in `LoginLogoutServiceImpl` (`plusHours(1)`).

**Refactor:**

- Add `application.properties` (or profile-specific) entries, e.g.:
  - `app.session.duration-hours=1`
- Inject via `@Value("${app.session.duration-hours}") int sessionDurationHours` and use in session creation.
- Optionally move token prefix and other auth constants to config as well.

### 8.2 Date format for orders

**Issue:** Order-by-date uses a hardcoded pattern `dd-MM-yyyy` in `OrderController`.

**Refactor:**

- Use a constant or config property (e.g. `app.date.format=dd-MM-yyyy`) and inject/formatter in one place so it can be reused and changed without code change.

---

## 9. Code Style & Consistency

### 9.1 Naming

**Issue:** Inconsistent naming: `pService`, `sService`, `oDao`, `cartservicei`, `cs`, `customerdto`, etc.

**Refactor:**

- Use clear, consistent names: `productService`, `sellerService`, `orderService`, `cartService`, `customerService`.
- Same for DTOs: `customerDto`, `orderDto` (or `createOrderRequest`) instead of `odto` / `orderdto`.

### 9.2 Commented-out code

**Issue:** Commented code in `OrderServiceImpl` (e.g. repeated `getLoggedInCustomerDetails`).

**Refactor:**

- Remove commented-out code; rely on version control for history.

### 9.3 Javadoc / API documentation

**Issue:** Many public service and controller methods lack clear descriptions; some controllers have inline comments instead of structured docs.

**Refactor:**

- Add short Javadoc for public service methods (purpose, parameters, return, exceptions).
- Use OpenAPI/Swagger annotations where needed (`@Operation`, `@ApiResponse`) so API docs stay in sync with behavior.

---

## 10. Testing & Maintainability

### 10.1 Testability

**Issue:** Heavy use of `@Autowired` and direct instantiation of dependencies makes unit testing harder.

**Refactor:**

- Constructor injection (see 3.4) allows passing mocks in tests without Spring.
- Centralized token validation (see 2.1) allows mocking one component for “logged-in user” scenarios.

### 10.2 Transaction boundaries

**Issue:** Some service methods are `@Transactional`; others are not. Large transactions can hold connections longer and increase lock contention.

**Refactor:**

- Review which methods modify data and need a transaction; mark only those `@Transactional` with an explicit propagation if needed.
- Keep read-only operations non-transactional or use `@Transactional(readOnly = true)` where appropriate.

---

## Suggested order of work

| Priority | Area                         | Effort | Impact |
|----------|------------------------------|--------|--------|
| 1        | Remove duplicate DTOs (1.1) | Low    | Clarity, fewer bugs |
| 2        | Centralize token validation (2.1, 2.2) | Medium | Security, DRY, fewer NPEs |
| 3        | Remove OrderDao from controller (3.1) | Low | Clean layering |
| 4        | Fix validation → 400, Category → 404 (4.1, 4.2) | Low | Correct HTTP semantics |
| 5        | OrderServiceImpl → depend on CartService (5.1) | Low | Better design |
| 6        | Replace Optional.get() (2.1, 5.2) | Medium | Stability |
| 7        | Single seller registration endpoint (3.3) | Low | Clear API |
| 8        | Response/request DTOs for API (7.1, 7.2) | High | Stable, clear API |
| 9        | Constructor injection (3.4), naming (9.1) | Medium | Testability, readability |
| 10       | Repository rename Dao → Repository (6.1) | Low | Convention |

You can tackle these in small PRs: e.g. first DTO cleanup and token util, then controller/exception fixes, then service cleanup and DTOs for API.
