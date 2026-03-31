# E-Commerce Backend — Spring Boot

A production-grade REST API for an e-commerce platform with JWT authentication, product/cart/order management, and Stripe payment integration.

---

## 🚀 Quick Start (Spring Tool Suite / IntelliJ / CLI)

### Prerequisites
- Java 17+
- Maven 3.8+

### Run in STS
1. **Import project**: File → Import → Maven → Existing Maven Projects → select this folder
2. **Wait** for Maven to download dependencies (~1 min first time)
3. Right-click `EcommerceApplication.java` → Run As → Spring Boot App
4. App starts at `http://localhost:8080`
5. H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:ecommercedb`, user: `sa`, password: blank)

### Run via CLI
```bash
mvn spring-boot:run
```

### Run Tests
```bash
mvn test
```

---

## 🔑 Pre-seeded Accounts

| Username   | Password    | Role        |
|------------|-------------|-------------|
| `admin`    | `Admin@123` | ADMIN, USER |
| `testuser` | `Test@123`  | USER        |

---

## 📋 API Endpoints

### Auth
| Method | URL                    | Access  | Description       |
|--------|------------------------|---------|-------------------|
| POST   | `/api/auth/register`   | Public  | Register new user |
| POST   | `/api/auth/login`      | Public  | Login, get JWT    |

### Products
| Method | URL                              | Access     | Description          |
|--------|----------------------------------|------------|----------------------|
| GET    | `/api/products`                  | Public     | List all products    |
| GET    | `/api/products/{id}`             | Public     | Get product by ID    |
| GET    | `/api/products/search?name=xyz`  | Public     | Search by name       |
| GET    | `/api/products/category/{cat}`   | Public     | Filter by category   |
| POST   | `/api/products`                  | ADMIN only | Create product       |
| PUT    | `/api/products/{id}`             | ADMIN only | Update product       |
| DELETE | `/api/products/{id}`             | ADMIN only | Delete product       |

### Cart (requires JWT)
| Method | URL                      | Description           |
|--------|--------------------------|-----------------------|
| GET    | `/api/cart`              | Get my cart           |
| POST   | `/api/cart/items`        | Add item to cart      |
| PUT    | `/api/cart/items/{id}`   | Update item quantity  |
| DELETE | `/api/cart/items/{id}`   | Remove item from cart |
| DELETE | `/api/cart`              | Clear entire cart     |

### Orders (requires JWT)
| Method | URL                        | Access     | Description              |
|--------|----------------------------|------------|--------------------------|
| POST   | `/api/orders`              | USER       | Place order from cart    |
| GET    | `/api/orders`              | USER       | Get my orders            |
| GET    | `/api/orders/{id}`         | USER/ADMIN | Get order details        |
| GET    | `/api/orders/admin/all`    | ADMIN only | Get all orders           |
| PUT    | `/api/orders/{id}/status`  | ADMIN only | Update order status      |

### Payments (requires JWT)
| Method | URL                                    | Description                 |
|--------|----------------------------------------|-----------------------------|
| POST   | `/api/payments/create-intent`          | Create Stripe PaymentIntent |
| POST   | `/api/payments/confirm/{intentId}`     | Confirm payment             |
| POST   | `/api/payments/refund/{orderId}`       | Refund an order             |

---

## 🔐 Authentication

All protected endpoints require an `Authorization` header:
```
Authorization: Bearer <your_jwt_token>
```

---

## 💳 Stripe Integration

- **Test mode**: Add your Stripe test key in `application.properties`:
  ```properties
  stripe.api.key=sk_test_YOUR_KEY_HERE
  ```
- **Without a Stripe key**: The app uses a built-in mock fallback — all payment flows work with mock intent IDs starting with `pi_mock_`.

---

## 📦 Typical Workflow

```
1. POST /api/auth/login              → get JWT token
2. GET  /api/products                → browse products
3. POST /api/cart/items              → add items to cart
4. POST /api/orders                  → place order
5. POST /api/payments/create-intent  → get clientSecret
6. POST /api/payments/confirm/{id}   → confirm payment
```

---

## 🏗️ Tech Stack

| Layer       | Technology                    |
|-------------|-------------------------------|
| Framework   | Spring Boot 3.2               |
| Security    | Spring Security + JWT (JJWT)  |
| Database    | H2 (in-memory, zero config)   |
| ORM         | Spring Data JPA / Hibernate   |
| Payments    | Stripe Java SDK               |
| Validation  | Jakarta Bean Validation       |
| Testing     | JUnit 5, Mockito, MockMvc     |
| Utilities   | Lombok                        |

---

## 🧪 Test Coverage

- `AuthServiceTest` — register, login, duplicate checks
- `ProductServiceTest` — CRUD, search, validation
- `CartServiceTest` — add/update/remove items, stock checks, ownership
- `OrderServiceTest` — create/view orders, status updates, stock deduction
- `PaymentServiceTest` — intent creation, confirm, refund (mock Stripe fallback)
- `JwtUtilsTest` — token generation, validation, extraction
- `AuthControllerTest` — endpoint contract tests via MockMvc
- `ProductControllerTest` — role-based access via MockMvc
- `EcommerceApplicationTests` — full Spring context load
