# Brokage Firm API

A backend REST API for a brokerage firm that enables employees to manage stock orders for their customers.

## Features

- **Order Management**: Create, list, and cancel stock orders
- **Asset Management**: Track customer assets with usable size management  
- **Authentication**: JWT-based authentication with role-based access control
- **Admin Functions**: Match pending orders

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA
- H2 Database
- Maven
- Docker

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker (optional)

### Run with Maven

```bash
cd brokage-api
mvn spring-boot:run
```

### Run with Docker

```bash
cd brokage-api
docker-compose up -d --build
```

The API will be available at `http://localhost:8080`

## Default Users

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| customer1 | pass123 | CUSTOMER |

## API Endpoints

### Authentication

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"password123"}'
```

### Orders

```bash
# Create Order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"customerId":2,"assetName":"AAPL","orderSide":"BUY","size":10,"price":150}'

# List Orders
curl http://localhost:8080/api/orders?customerId=2 \
  -H "Authorization: Bearer <token>"

# Cancel Order
curl -X DELETE http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer <token>"
```

### Assets

```bash
# List Assets
curl http://localhost:8080/api/assets?customerId=2 \
  -H "Authorization: Bearer <token>"
```

### Admin

```bash
# Match Order
curl -X POST http://localhost:8080/api/admin/orders/1/match \
  -H "Authorization: Bearer <token>"

# List All Orders
curl http://localhost:8080/api/admin/orders \
  -H "Authorization: Bearer <token>"
```

## Business Rules

1. Orders are created with `PENDING` status
2. BUY orders require sufficient TRY balance
3. SELL orders require sufficient asset balance
4. Only `PENDING` orders can be canceled
5. Canceled orders restore the reserved amount
6. Matched orders update both size and usableSize
7. Customers can only access their own data

## Running Tests

```bash
mvn test
```

## H2 Console

Access the database console at `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:brokagedb`
- Username: `sa`
- Password: (empty)

## Project Structure

```
src/main/java/com/brokage/
├── config/          # Security and app configuration
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
├── enums/           # OrderSide, OrderStatus
├── exception/       # Custom exceptions and handlers
├── repository/      # JPA repositories
├── security/        # JWT authentication
├── service/         # Business logic
└── validation/      # Custom validators
```

## License

This project is for case study purposes.
