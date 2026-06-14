# ShopEasy — E-Commerce Spring Boot App

A beginner-friendly full-stack e-commerce web app: browse products, add to cart, checkout, view order history.

## Tech Stack
- Java 17 + Spring Boot 3
- MySQL 8 + Spring Data JPA
- Spring Security (BCrypt)
- HTML/CSS/JavaScript frontend

## Project Structure
```
src/main/java/com/ecommerce/
├── EcommerceApplication.java
├── config/SecurityConfig.java
├── model/      (User, Product, CartItem, Order, OrderItem)
├── repository/ (UserRepository, ProductRepository, CartItemRepository, OrderRepository, OrderItemRepository)
├── service/    (UserService, ProductService, CartService, OrderService)
└── controller/ (UserController, ProductController, CartController, OrderController)

src/main/resources/
├── application.properties
└── static/ (index.html, css/style.css, js/app.js)
```

---

## Step-by-Step Setup & Deployment

### Step 1 — Install prerequisites
```bash
java -version    # Need Java 17+
mvn -version      # Need Maven 3.8+
mysql --version    # Need MySQL 8
```

### Step 2 — Create the database
```bash
mysql -u root -p
```
```sql
CREATE DATABASE ecommerce_db;
CREATE USER 'ecom_user'@'localhost' IDENTIFIED BY 'StrongPass123';
GRANT ALL PRIVILEGES ON ecommerce_db.* TO 'ecom_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```
Tables (`users`, `products`, `cart_items`, `orders`, `order_items`) are auto-created by Hibernate on first run.

### Step 3 — Configure credentials
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=ecom_user
spring.datasource.password=StrongPass123
```

### Step 4 — Build the project
```bash
cd ecommerce
mvn clean package
```
This creates `target/ecommerce-app-1.0.0.jar`

### Step 5 — Run the app
```bash
# Option A: via Maven
mvn spring-boot:run

# Option B: run the JAR directly
java -jar target/ecommerce-app-1.0.0.jar
```
Visit **http://localhost:8080**

### Step 6 — Seed some products (optional)
Insert sample data directly into MySQL so the storefront isn't empty:
```sql
USE ecommerce_db;
INSERT INTO products (name, description, price, stock, category, image_url) VALUES
('Wireless Mouse', 'Ergonomic 2.4GHz wireless mouse', 799.00, 50, 'Electronics', ''),
('Cotton T-Shirt', '100% cotton, unisex', 499.00, 100, 'Clothing', ''),
('Java Programming Book', 'Beginner to advanced Java guide', 599.00, 30, 'Books', ''),
('Table Lamp', 'LED desk lamp with adjustable brightness', 1299.00, 20, 'Home', '');
```

### Step 7 — Run tests
```bash
mvn test
```

---

## Using the App

1. Open `http://localhost:8080` → Register a new account
2. Browse products on the **Products** tab, search/filter by category
3. Click **Add to Cart**
4. Go to **Cart** tab → adjust quantities → enter shipping address → **Place Order**
5. Go to **Orders** tab → view order history, cancel pending orders

---

## REST API Reference

### Users
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users/register` | Register new user |
| GET | `/api/users/{id}` | Get user profile |
| PUT | `/api/users/{id}/profile` | Update address/phone |

### Products
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | List all products |
| GET | `/api/products?name=x&category=y` | Search/filter |
| GET | `/api/products/in-stock` | In-stock only |
| GET | `/api/products/{id}` | Get one product |
| POST | `/api/products` | Add product (admin) |
| DELETE | `/api/products/{id}` | Delete product (admin) |

### Cart
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/cart?userId=1` | Get cart items |
| GET | `/api/cart/total?userId=1` | Get cart total |
| POST | `/api/cart` | Add item to cart |
| PUT | `/api/cart/{id}` | Update quantity |
| DELETE | `/api/cart/{id}?userId=1` | Remove item |

### Orders
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders/checkout` | Place order from cart |
| GET | `/api/orders?userId=1` | Order history |
| GET | `/api/orders/{id}?userId=1` | Get one order |
| PATCH | `/api/orders/{id}/cancel?userId=1` | Cancel order |
| PATCH | `/api/orders/{id}/status` | Update status (admin) |

---

## How to explain this project in an interview

> "I built a full-stack e-commerce application using Java Spring Boot and MySQL.
> It has product browsing with search and category filters, a shopping cart system,
> and a checkout flow that creates orders, snapshots prices at purchase time, and
> reduces product stock transactionally. I used Spring Security with BCrypt for
> authentication, and structured the code in clean MVC layers — entities, repositories,
> services, and REST controllers."

**Key talking points:**
- `@Transactional` checkout ensures stock reduction + order creation + cart clearing all succeed or all roll back together
- Price snapshot in `OrderItem` (prices can change later without affecting past orders)
- Ownership checks in CartService/OrderService (users can't access others' carts/orders)
- BigDecimal used for money (never float/double)
- RESTful API design across 4 resource types
