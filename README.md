# ShopStream — Microservices E-Commerce Backend

Production-grade e-commerce backend built with Java 21,
Spring Boot 3, and distributed systems patterns.

# 🏗️ System Architecture

```text
                           ┌────────────────────┐
                           │       Client       │
                           │  (Frontend/App)    │
                           └─────────┬──────────┘
                                     │
                                     ▼
                    ┌────────────────────────────────┐
                    │        API Gateway             │
                    │    JWT Authentication Layer    │
                    └────────────────┬───────────────┘
                                     │
                                     ▼
                    ┌────────────────────────────────┐
                    │     Eureka Service Registry    │
                    │       Service Discovery        │
                    └────────────────┬───────────────┘
                                     │
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
          ▼                          ▼                          ▼

┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│ Product Service │      │  Order Service  │      │   Auth Service  │
└────────┬────────┘      └────────┬────────┘      └────────┬────────┘
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   PostgreSQL    │      │   PostgreSQL    │      │   PostgreSQL    │
└─────────────────┘      └─────────────────┘      └─────────────────┘
         │
         ▼
┌─────────────────┐
│      Redis      │
│ Product Caching │
└─────────────────┘

                          ┌─────────────────┐
                          │      Kafka      │
                          │ Event Streaming │
                          └────────┬────────┘
                                   │
             ┌─────────────────────┴─────────────────────┐
             ▼                                           ▼

┌─────────────────────────┐          ┌─────────────────────────┐
│   Inventory Consumer    │          │ Notification Consumer   │
└─────────────────────────┘          └─────────────────────────┘

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Service Discovery | Eureka |
| API Gateway | Spring Cloud Gateway |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Messaging | Apache Kafka |
| Security | JWT + Spring Security |
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions |

## Key Features

- **Event-driven architecture** — services communicate via Kafka, zero tight coupling
- **JWT security** — validated at gateway level, services receive user context as headers
- **Redis caching** — product catalog cached with TTL, 75% DB load reduction
- **Database per service** — complete data isolation between services
- **Soft deletes** — audit trail preserved, referential integrity maintained
- **1000+ seeded products** — realistic test data

## Running Locally

```bash
# Start infrastructure
docker-compose up -d

# Start services (in order)
cd service-registry && mvn spring-boot:run
cd config-server    && mvn spring-boot:run
cd api-gateway      && mvn spring-boot:run
cd product-service  && mvn spring-boot:run
cd order-service    && mvn spring-boot:run
cd auth-service     && mvn spring-boot:run
```

## API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create a new user account |
| POST | `/api/auth/login` | Authenticate user and generate JWT token |

### Products (requires JWT Authentication)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | Fetch all products *(Redis Cached)* |
| GET | `/api/products/{id}` | Fetch product by ID |
| GET | `/api/products/search?keyword=` | Search products |
| POST | `/api/products` | Create new product *(ADMIN only)* |
| PUT | `/api/products/{id}` | Update existing product *(ADMIN only)* |
| DELETE | `/api/products/{id}` | Soft delete product *(ADMIN only)* |


### Orders (requires JWT Authentication)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders` | Place order *(Triggers Kafka Event)* |
| GET | `/api/orders/{id}` | Fetch order by ID |
| GET | `/api/orders/customer/{email}` | Fetch customer order history |
| PATCH | `/api/orders/{id}/status` | Update order status |

## Running Tests

```bash
mvn test
```

## CI/CD

Every push to main triggers GitHub Actions:
1. Build all 6 modules
2. Run unit tests
3. Generate coverage report