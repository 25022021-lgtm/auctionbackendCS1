# Auction Platform Backend

This repository contains the backend for a real-time auction platform. It provides a comprehensive RESTful API for managing user accounts, funding balances, listing auction items, placing real-time bids, and streaming live price updates using Server-Sent Events (SSE).

The backend is built using **Spring Boot**, **Spring Security**, **JPA/Hibernate**, and **SQLite**.

---

## Features

*   **User Authentication & Security:**
    *   User registration and secure login.
    *   JWT-based stateless authentication (`Bearer` tokens).
    *   Refresh token mechanism to maintain seamless active user sessions.
*   **User Profile & Balance Management:**
    *   Deposit credits into user balances.
    *   Query current balance and profile details.
*   **Item & Auction Management:**
    *   Publish items for auction with customized starting price, buy-it-now price, bid increment, and auction duration.
    *   Retrieve all active auction listings via paginated lists.
    *   Cancel active auctions before they receive bids.
*   **Real-time Bidding Orchestration:**
    *   Place bids on items with automatic validation (must satisfy starting price, bid increment, and seller restrictions).
    *   Anti-sniping logic: Automatically extends auction end time if bids are placed close to expiration.
    *   Lock and unlock funds dynamically: Automatically deducts additional funds for higher bids and refunds previous bidders.
    *   Support for immediate purchase via "Buy Now" at the buy-it-now price.
*   **Real-time Live Price Streaming:**
    *   Leverages Reactive Spring WebFlux and Server-Sent Events (SSE) to stream real-time price updates to connected clients.

---

## Technologies Used

*   **Core Framework:** Spring Boot
*   **Database:** SQLite (embedded)
*   **ORM / JPA:** Spring Data JPA / Hibernate
*   **Security:** Spring Security, JSON Web Tokens (JWT)
*   **Reactive Streaming:** Project Reactor (Flux/Mono) for Server-Sent Events (SSE)
*   **API Documentation:** Swagger UI (Springdoc OpenAPI)

---

## Getting Started

### Prerequisites

*   Java 26 (or compatible JDK version)
*   Gradle (wrapper included in project)

### Running the Application

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/auction-backend.git
    cd auction-backend
    ```

2.  **Build and run using Gradle wrapper:**
    ```bash
    ./gradlew bootRun
    ```

The application starts on port `8080` by default.

---

## API Documentation & Explorer

Interactive API documentation and playground are available via Swagger UI. Once the backend is running, open your browser and navigate to:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Endpoint Reference

### 1. Authentication Endpoints
*   `POST /register` - Register a new account.
*   `POST /login` - Log in and obtain an access token and refresh token.
*   `POST /refresh` - Request a new access token using a valid refresh token.

### 2. User & Balance Endpoints
*   `GET /users/me` - Get profile details of the authenticated user.
*   `GET /users/me/balance` - Retrieve current wallet balance.
*   `POST /users/me/deposit` - Deposit credits into the user balance.

### 3. Item Endpoints
*   `POST /items` - Publish a new item for auction.
*   `GET /items` - Get a paginated list of active items.
*   `GET /items/{itemId}` - Retrieve details of a specific item.
*   `GET /items/listings/{username}` - Get all auction listings published by a specific user.
*   `POST /items/cancel/{itemId}` - Cancel an active auction listing (seller only).
*   `GET /items/all` - Retrieve all items (testing/dev endpoint).

### 4. Bidding & Auction Orchestration Endpoints
*   `POST /bid` - Place a bid on an active auction item.
*   `POST /buy-now/{itemId}` - Purchase an item immediately at the Buy-It-Now price.
*   `GET /me/bids` - Retrieve active bids placed by the authenticated user.
*   `GET /me/wins` - Retrieve items won by the authenticated user.
*   `GET /bids/{itemId}/bids` - Get the bid history for a specific item.
*   `GET /items/stream/{itemId}` - SSE endpoint to stream real-time price updates for an item.

---

## System Architecture

The application is structured into clearly separated layers:

1.  **Controller Layer (`*Controller.java`):** Exposes REST and SSE endpoints, processes HTTP requests, validates inputs, and maps responses.
2.  **Service Layer (`*Service.java`):** Implements business logic, orchestrates transactional boundaries, manages entity state transitions, and publishes event updates.
3.  **Repository Layer (`*Repository.java`):** Interfaces with SQLite using Spring Data JPA.
4.  **Security Config (`SecurityConfig.java`):** Configures security filters, password hashing, and authentication requirements for protected API endpoints.
