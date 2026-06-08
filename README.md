# 🏦 Banking Application - Backend

A comprehensive, secure, Spring Boot-based backend REST API for a full-featured banking system. This application provides all the server-side logic for managing user accounts, transactions, loans, fixed deposits, cards, beneficiaries, and support tickets, with enterprise-grade security features.

---

## 📚 Table of Contents

1. [Project Overview](#-project-overview)
2. [Tech Stack](#-tech-stack)
3. [Project Structure (Explained Step-by-Step)](#-project-structure-explained-step-by-step)
4. [Security Architecture](#-security-architecture)
5. [Prerequisites](#-prerequisites)
6. [Setup Instructions (Step-by-Step)](#-setup-instructions-step-by-step)
7. [Running the Application](#️-running-the-application)
8. [API Endpoints (Detailed)](#-api-endpoints-detailed)
9. [Database Schema Overview](#-database-schema-overview)
10. [Seed Data & Test Credentials](#-seed-data--test-credentials)
11. [Environment Variables & Configuration](#-environment-variables--configuration)
12. [Troubleshooting](#-troubleshooting)

---

## 🌟 Project Overview

This is the **backend** of a banking application built with **Spring Boot**. It handles all business logic, data persistence, authentication, authorization, and exposes RESTful APIs that a frontend client (like the `frontend/` React app in this project) can consume.

**Core Capabilities:**
- User registration, login, and role-based access control (User / Admin)
- Account management (Savings, Current, Business accounts)
- Fund transfers between accounts
- Beneficiary management for quick transfers
- Loan applications with admin approval workflow
- Fixed Deposit creation and management
- Debit/Credit card management (apply, activate, freeze)
- Customer support ticket system with reply tracking
- Full audit logging for compliance
- OTP-based two-factor authentication
- JWT token-based session management
- Rate limiting to prevent brute-force attacks

---

## 🚀 Tech Stack

| Technology          | Purpose                          |
|---------------------|----------------------------------|
| **Java 17**         | Core programming language        |
| **Spring Boot 3.x** | Application framework            |
| **Spring Security** | Authentication & authorization   |
| **Spring Data JPA** | Database access (ORM)            |
| **Hibernate**       | ORM implementation (JPA provider)|
| **MySQL 8**         | Relational database              |
| **Maven**           | Build & dependency management    |
| **JWT (jjwt)**      | JSON Web Token generation/validation |
| **Lombok**          | Reducing boilerplate code        |
| **Jakarta Mail**    | Email sending (OTP, notifications)|

---

## 📁 Project Structure (Explained Step-by-Step)

```
myapp/
│
├── pom.xml                          # Maven build file (dependencies, plugins)
├── mvnw / mvnw.cmd                  # Maven wrapper scripts (no need to install Maven globally)
│
├── src/
│   ├── main/
│   │   ├── java/com/example/banking/
│   │   │   │
│   │   │   ├── BankingApplication.java    # Main entry point (@SpringBootApplication)
│   │   │   │
│   │   │   ├── common/                    # Shared utilities across the project
│   │   │   │   ├── ApiResponse.java       # Standardized API response wrapper
│   │   │   │   ├── CorsConfig.java        # CORS configuration for frontend access
│   │   │   │   ├── GlobalExceptionHandler.java  # Centralized error handling
│   │   │   │   ├── ReferenceNumberGenerator.java # Generates unique reference numbers
│   │   │   │   ├── UserUtil.java          # Helper methods for current user context
│   │   │   │   ├── SeedDataRunner.java    # Seeds initial data on first run
│   │   │   │   └── (AccountStatus, AccountType, TransactionType, 
│   │   │   │        LoanStatus, RoleType)  # Enum definitions
│   │   │   │
│   │   │   ├── user/                      # User management module
│   │   │   │   ├── User.java              # User entity (JPA)
│   │   │   │   ├── Role.java              # Role entity (JPA)
│   │   │   │   ├── UserRepository.java    # Database operations for users
│   │   │   │   ├── RoleRepository.java    # Database operations for roles
│   │   │   │   ├── AuthService.java       # Registration & login business logic
│   │   │   │   └── AuthController.java    # REST endpoints: /api/auth/**
│   │   │   │
│   │   │   ├── security/                  # Security & authentication layer
│   │   │   │   ├── SecurityConfig.java    # Spring Security configuration
│   │   │   │   ├── JwtService.java        # JWT token creation & validation
│   │   │   │   ├── JwtAuthFilter.java     # Filters every request for JWT
│   │   │   │   ├── CustomUserDetailsService.java # Loads user from database
│   │   │   │   ├── OtpService.java        # OTP generation & verification
│   │   │   │   ├── EmailService.java      # Sends emails (OTP, notifications)
│   │   │   │   ├── SessionService.java    # Active session management
│   │   │   │   ├── AuditService.java      # Logs all critical operations
│   │   │   │   ├── RateLimiter.java       # Prevents excessive requests
│   │   │   │   ├── TokenBlacklistService.java # Handles token revocation
│   │   │   │   ├── DeviceParser.java      # Parses device info from requests
│   │   │   │   └── (AuditLog, BlacklistedToken, LoginHistory, 
│   │   │   │        LoginSession, OtpToken, PasswordResetToken, 
│   │   │   │        RefreshToken)         # Security-related JPA entities
│   │   │   │   └── (AuditLogRepository, BlacklistedTokenRepository,
│   │   │   │        LoginHistoryRepository, LoginSessionRepository,
│   │   │   │        OtpTokenRepository, PasswordResetTokenRepository,
│   │   │   │        RefreshTokenRepository) # Security-related repositories
│   │   │   │
│   │   │   ├── account/                   # Account management module
│   │   │   │   ├── Account.java           # Account entity
│   │   │   │   ├── AccountRepository.java # Database operations
│   │   │   │   ├── AccountService.java    # Business logic
│   │   │   │   └── AccountController.java # REST endpoints: /api/accounts/**
│   │   │   │
│   │   │   ├── transaction/               # Transaction module
│   │   │   │   ├── Transaction.java       # Transaction entity
│   │   │   │   ├── TransactionRepository.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   └── TransactionController.java # /api/transactions/**
│   │   │   │
│   │   │   ├── beneficiary/               # Beneficiary module
│   │   │   │   ├── Beneficiary.java
│   │   │   │   ├── BeneficiaryRepository.java
│   │   │   │   ├── BeneficiaryService.java
│   │   │   │   └── BeneficiaryController.java # /api/beneficiaries/**
│   │   │   │
│   │   │   ├── loan/                      # Loan management module
│   │   │   │   ├── Loan.java
│   │   │   │   ├── LoanRepository.java
│   │   │   │   ├── LoanService.java
│   │   │   │   └── LoanController.java    # /api/loans/**
│   │   │   │
│   │   │   ├── fd/                        # Fixed Deposit module
│   │   │   │   ├── FixedDeposit.java
│   │   │   │   ├── FixedDepositRepository.java
│   │   │   │   ├── FixedDepositService.java
│   │   │   │   └── FDController.java      # /api/fixed-deposits/**
│   │   │   │
│   │   │   ├── card/                      # Card management module
│   │   │   │   ├── Card.java
│   │   │   │   ├── CardRepository.java
│   │   │   │   ├── CardService.java
│   │   │   │   └── CardController.java    # /api/cards/**
│   │   │   │
│   │   │   ├── support/                   # Support ticket module
│   │   │   │   ├── SupportTicket.java
│   │   │   │   ├── SupportTicketRepository.java
│   │   │   │   ├── SupportTicketService.java
│   │   │   │   └── SupportTicketController.java # /api/support-tickets/**
│   │   │   │
│   │   │   └── admin/                     # Admin-only operations
│   │   │       ├── AdminService.java
│   │   │       └── AdminController.java   # /api/admin/**
│   │   │
│   │   └── resources/
│   │       └── application.properties     # Database & app configuration
│   │
│   └── test/                              # Unit & integration tests (to be added)
│
└── TODO.md                                # Project roadmap & pending tasks
```

---

## 🔐 Security Architecture

The application implements a **multi-layered security approach**:

### Step 1: Authentication Flow
```
User enters credentials → AuthController.login() 
  → AuthService authenticates (validates email + password)
    → JwtService generates an Access Token (short-lived, ~15 min)
    → JwtService generates a Refresh Token (long-lived, ~7 days)
    → Returns both tokens + user profile to the client
```

### Step 2: Request Authorization Flow
```
Client sends request with Bearer token in Authorization header
  → JwtAuthFilter intercepts the request
    → Validates JWT signature & expiration
    → Extracts user email from token claims
    → Loads user details from database
    → Sets authentication context in SecurityContextHolder
    → Request proceeds to the Controller
```

### Step 3: Additional Security Measures
1. **OTP Verification** — Required for sensitive operations (large transfers, password reset)
2. **Rate Limiting** — Maximum N requests per minute per user/IP to prevent brute force
3. **Token Blacklisting** — Revoked tokens are stored and checked on every request
4. **Session Management** — Tracks active login sessions, allows force-logout
5. **Audit Logging** — Every critical operation is logged with timestamp, user, IP, action
6. **Password Reset Flow** — Email-based reset with expiring tokens
7. **Role-Based Access Control** — `USER` and `ADMIN` roles with different permission levels

---

## ⚙️ Prerequisites

Before you begin, ensure you have the following installed:

| Tool          | Version | Check Command           | Notes                         |
|---------------|---------|-------------------------|-------------------------------|
| **JDK**       | 17+     | `java -version`         | OpenJDK or Oracle JDK         |
| **Maven**     | 3.6+    | `mvn -version`          | Or use bundled `mvnw`         |
| **MySQL**     | 8+      | `mysql --version`       | Database server must be running |
| **Git**       | Any     | `git --version`         | For cloning the repository    |

---

## 🛠️ Setup Instructions (Step-by-Step)

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd Spring\ java/myapp
```

### Step 2: Configure MySQL Database
Open your MySQL client (command line, MySQL Workbench, or any GUI tool) and create a new database:
```sql
CREATE DATABASE banking_db;
```

### Step 3: Configure Application Properties
Open `src/main/resources/application.properties` and update the following:
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/banking_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root          # Replace with your MySQL username
spring.datasource.password=yourpassword  # Replace with your MySQL password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update     # Auto-creates/updates tables
spring.jpa.show-sql=true                 # Shows SQL in console (disable in production)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT Configuration
jwt.secret=your-256-bit-secret-key-here-must-be-very-long-and-secure
jwt.expiration=900000                    # Access token expiry: 15 minutes (in ms)
jwt.refresh.expiration=604800000         # Refresh token expiry: 7 days (in ms)

# Email Configuration (for OTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Step 4: Build the Project
```bash
# Using Maven wrapper (no global Maven install needed)
./mvnw clean install

# OR using globally installed Maven
mvn clean install
```
This command will:
1. Download all dependencies (Spring Boot, JPA, Security, JWT, MySQL connector, etc.)
2. Compile all Java source files
3. Run tests (if available)
4. Package the application into a JAR file inside the `target/` folder

### Step 5: Run the Application
```bash
# Option A: Using Maven
./mvnw spring-boot:run

# Option B: Using the generated JAR
java -jar target/banking-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**.

---

## ▶️ Running the Application

### Development Mode
```bash
# Run with hot-reload (auto-restarts on code changes)
mvn spring-boot:run -Dspring-boot.run.fork=true
```

### Production Mode
```bash
# Build and run the JAR
mvn clean package -DskipTests
java -jar target/banking-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Verify the Application is Running
Open your browser or use curl:
```bash
curl http://localhost:8080/api/auth/health
# Expected response: {"status":"UP","message":"Banking Application is running"}
```

---

## 📋 API Endpoints (Detailed)

### 🔑 Authentication (`/api/auth`)

| Method | Endpoint            | Description               | Auth Required | Role   |
|--------|---------------------|---------------------------|---------------|--------|
| POST   | `/register`         | Create a new user account | No            | -      |
| POST   | `/login`            | Authenticate & get tokens | No            | -      |
| POST   | `/refresh`          | Get new access token      | Refresh Token | -      |
| POST   | `/logout`           | Invalidate current session| Yes           | ANY    |
| POST   | `/forgot-password`  | Send password reset email | No            | -      |
| POST   | `/reset-password`   | Reset password with token | No            | -      |
| POST   | `/send-otp`         | Send OTP to email         | Yes           | ANY    |
| POST   | `/verify-otp`       | Verify OTP code           | Yes           | ANY    |

### 👤 User Profile (`/api/users`)

| Method | Endpoint   | Description              | Auth Required | Role   |
|--------|------------|--------------------------|---------------|--------|
| GET    | `/profile` | Get current user profile | Yes           | ANY    |
| PUT    | `/profile` | Update user profile      | Yes           | ANY    |
| PUT    | `/password`| Change password          | Yes           | ANY    |

### 💰 Accounts (`/api/accounts`)

| Method | Endpoint       | Description                  | Auth Required | Role   |
|--------|----------------|------------------------------|---------------|--------|
| GET    | `/`            | List all user's accounts      | Yes           | ANY    |
| POST   | `/`            | Create a new account          | Yes           | ANY    |
| GET    | `/{id}`        | Get account details           | Yes           | ANY    |
| GET    | `/{id}/balance`| Get account balance           | Yes           | ANY    |
| GET    | `/{id}/statement`| Get account statement        | Yes           | ANY    |
| PUT    | `/{id}/status` | Update account status (Admin) | Yes           | ADMIN  |

### 💸 Transactions (`/api/transactions`)

| Method | Endpoint          | Description                  | Auth Required | Role   |
|--------|-------------------|------------------------------|---------------|--------|
| GET    | `/`               | List user's transactions     | Yes           | ANY    |
| POST   | `/transfer`       | Transfer funds to another account | Yes     | ANY    |
| POST   | `/deposit`        | Deposit money into account   | Yes           | ANY    |
| POST   | `/withdraw`       | Withdraw money from account  | Yes           | ANY    |
| GET    | `/{reference}`    | Get transaction by reference | Yes           | ANY    |

### 👥 Beneficiaries (`/api/beneficiaries`)

| Method | Endpoint   | Description             | Auth Required | Role   |
|--------|------------|-------------------------|---------------|--------|
| GET    | `/`        | List all beneficiaries  | Yes           | ANY    |
| POST   | `/`        | Add a new beneficiary   | Yes           | ANY    |
| PUT    | `/{id}`    | Update beneficiary      | Yes           | ANY    |
| DELETE | `/{id}`    | Remove beneficiary      | Yes           | ANY    |

### 🏦 Loans (`/api/loans`)

| Method | Endpoint          | Description                | Auth Required | Role   |
|--------|-------------------|----------------------------|---------------|--------|
| GET    | `/`               | List user's loans          | Yes           | ANY    |
| POST   | `/`               | Apply for a new loan       | Yes           | ANY    |
| GET    | `/{id}`           | Get loan details           | Yes           | ANY    |
| POST   | `/{id}/repay`     | Make a loan repayment      | Yes           | ANY    |
| PUT    | `/{id}/approve`   | Approve loan (Admin)       | Yes           | ADMIN  |
| PUT    | `/{id}/reject`    | Reject loan (Admin)        | Yes           | ADMIN  |

### 📈 Fixed Deposits (`/api/fixed-deposits`)

| Method | Endpoint          | Description                | Auth Required | Role   |
|--------|-------------------|----------------------------|---------------|--------|
| GET    | `/`               | List user's FDs            | Yes           | ANY    |
| POST   | `/`               | Create a new FD            | Yes           | ANY    |
| GET    | `/{id}`           | Get FD details             | Yes           | ANY    |
| POST   | `/{id}/withdraw`  | Withdraw FD on maturity    | Yes           | ANY    |
| POST   | `/{id}/renew`     | Renew FD for another term  | Yes           | ANY    |

### 💳 Cards (`/api/cards`)

| Method | Endpoint          | Description                | Auth Required | Role   |
|--------|-------------------|----------------------------|---------------|--------|
| GET    | `/`               | List user's cards          | Yes           | ANY    |
| POST   | `/`               | Apply for a new card       | Yes           | ANY    |
| GET    | `/{id}`           | Get card details           | Yes           | ANY    |
| PUT    | `/{id}/activate`  | Activate a card            | Yes           | ANY    |
| PUT    | `/{id}/freeze`    | Freeze a card              | Yes           | ANY    |
| PUT    | `/{id}/unfreeze`  | Unfreeze a card            | Yes           | ANY    |
| DELETE | `/{id}`           | Close/delete a card        | Yes           | ANY    |

### 🎫 Support Tickets (`/api/support-tickets`)

| Method | Endpoint          | Description                | Auth Required | Role   |
|--------|-------------------|----------------------------|---------------|--------|
| GET    | `/`               | List user's tickets        | Yes           | ANY    |
| POST   | `/`               | Create a support ticket    | Yes           | ANY    |
| GET    | `/{id}`           | Get ticket details         | Yes           | ANY    |
| POST   | `/{id}/reply`     | Reply to a ticket          | Yes           | ANY    |
| PUT    | `/{id}/status`    | Update ticket status       | Yes           | ADMIN  |

### 🔧 Admin (`/api/admin`)

| Method | Endpoint            | Description                 | Auth Required | Role   |
|--------|---------------------|-----------------------------|---------------|--------|
| GET    | `/users`            | List all users              | Yes           | ADMIN  |
| GET    | `/users/{id}`       | Get user details            | Yes           | ADMIN  |
| PUT    | `/users/{id}/role`  | Change user role            | Yes           | ADMIN  |
| PUT    | `/users/{id}/status`| Activate/deactivate user    | Yes           | ADMIN  |
| GET    | `/accounts`         | List all accounts           | Yes           | ADMIN  |
| GET    | `/transactions`     | List all transactions       | Yes           | ADMIN  |
| GET    | `/dashboard`        | Get dashboard statistics    | Yes           | ADMIN  |
| GET    | `/audit-logs`       | View audit logs             | Yes           | ADMIN  |

---

## 🗄️ Database Schema Overview

The application uses the following main database tables:

| Table                | Description                          | Key Fields                                      |
|----------------------|--------------------------------------|-------------------------------------------------|
| `users`              | Registered users                     | id, email, password, full_name, phone, enabled  |
| `roles`              | User roles                           | id, name (ROLE_USER, ROLE_ADMIN)                |
| `user_roles`         | Many-to-many user-role mapping       | user_id, role_id                                |
| `accounts`           | Bank accounts                        | id, account_number, balance, type, status, user_id |
| `transactions`       | Financial transactions               | id, reference, amount, type, from_account, to_account, timestamp |
| `beneficiaries`      | Saved beneficiaries for transfers    | id, name, account_number, user_id               |
| `loans`              | Loan applications                    | id, amount, interest_rate, tenure, status, user_id |
| `loan_repayments`    | Loan repayment records               | id, loan_id, amount, payment_date               |
| `fixed_deposits`     | Fixed deposit accounts               | id, amount, interest_rate, term_months, maturity_date, status |
| `cards`              | Debit/Credit cards                   | id, card_number, type, status, account_id, user_id |
| `support_tickets`    | Customer support tickets             | id, subject, description, status, user_id       |
| `ticket_replies`     | Replies to support tickets           | id, ticket_id, message, sender_type, timestamp  |
| `audit_logs`         | Security audit trail                 | id, action, user_id, ip_address, timestamp, details |
| `refresh_tokens`     | JWT refresh token storage            | id, token, user_id, expires_at, revoked         |
| `password_reset_tokens`| Password reset tokens              | id, token, user_id, expires_at, used            |
| `otp_tokens`         | OTP verification codes               | id, otp, user_id, purpose, expires_at, verified |
| `login_sessions`     | Active login sessions                | id, user_id, device_info, ip_address, last_active |
| `login_history`      | Login attempt history                | id, user_id, ip_address, success, failure_reason, timestamp |
| `blacklisted_tokens` | Revoked/expired JWT tokens           | id, token, blacklisted_at                       |

---

## 🐳 Seed Data & Test Credentials

On the **first application startup**, `SeedDataRunner.java` automatically inserts:

### Default Roles
- `ROLE_USER` — Standard banking customer
- `ROLE_ADMIN` — Administrator with full access

### Default Admin User
| Field    | Value            |
|----------|------------------|
| Email    | admin@banking.com |
| Password | Admin@123        |
| Role     | ADMIN            |

### Sample Test User
| Field    | Value           |
|----------|-----------------|
| Email    | user@banking.com |
| Password | User@123        |
| Role     | USER            |

### Sample Accounts
| Account Number | Type      | Balance | Owner    |
|---------------|-----------|---------|----------|
| ACC100001     | SAVINGS   | ₹50,000 | user     |
| ACC100002     | CURRENT   | ₹25,000 | user     |

---

## 🔧 Environment Variables & Configuration

All configuration is in `src/main/resources/application.properties`. Key properties:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/banking_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=your-super-secure-secret-key-at-least-256-bits-long
jwt.expiration=900000
jwt.refresh.expiration=604800000

# Email
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## ❗ Troubleshooting

### Issue 1: "Can't connect to MySQL"
- Ensure MySQL service is running: `net start mysql` (Windows) or `sudo systemctl start mysql` (Linux)
- Check if database `banking_db` exists
- Verify username/password in `application.properties`

### Issue 2: "Port 8080 already in use"
- Find the process using the port: `netstat -ano | findstr :8080` (Windows)
- Kill the process or change `server.port=8081` in `application.properties`

### Issue 3: "Invalid JWT signature"
- Your `jwt.secret` in `application.properties` might have changed
- Clear the database or regenerate tokens by logging in again

### Issue 4: Build fails with dependency errors
- Delete the local Maven repository cache: `rm -rf ~/.m2/repository` (Linux/Mac) or delete `C:\Users\{user}\.m2\repository` (Windows)
- Rebuild: `mvn clean install -U`

### Issue 5: Email not sending
- For Gmail, use an **App Password** (not your regular password)
- Enable "Less secure app access" or use OAuth2
- Check if port 587 is not blocked by your firewall

### Issue 6: Tables not created automatically
- Ensure `spring.jpa.hibernate.ddl-auto=update` is set
- Check MySQL user has CREATE/ALTER table permissions

---

## 🧪 Testing the APIs

You can test the APIs using **Postman**, **curl**, or any HTTP client:

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@example.com","password":"Pass@123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"Pass@123"}'

# Access a protected endpoint (replace TOKEN with the JWT from login response)
curl -X GET http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

---

## 📄 License

This project is for educational/demonstration purposes.

---

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

Built with ❤️ using Spring Boot