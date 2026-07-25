# Backend Guide - Banking Management System

This guide covers the backend architecture, API design, security implementation, and database configuration.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Domain Entities](#domain-entities)
- [Repository Layer](#repository-layer)
- [Service Layer](#service-layer)
- [Controller Layer](#controller-layer)
- [Security Implementation](#security-implementation)
- [Configuration](#configuration)
- [Database](#database)
- [API Documentation](#api-documentation)
- [Best Practices](#best-practices)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer (REST APIs)                               │
│  ├── AuthController      - Authentication endpoints         │
│  ├── AccountController   - Account operations               │
│  ├── TransactionController - Transaction processing         │
│  ├── AdminController     - Admin operations                 │
│  └── ...other controllers                                   │
├─────────────────────────────────────────────────────────────┤
│  Service Layer (Business Logic)                             │
│  ├── AuthService         - Login, register, token mgmt      │
│  ├── AccountService      - Account CRUD                     │
│  ├── TransactionService  - Deposit, withdraw, transfer      │
│  ├── AdminService        - Admin-specific logic             │
│  └── ...other services                                      │
├─────────────────────────────────────────────────────────────┤
│  Repository Layer (Data Access)                             │
│  ├── UserRepository                                         │
│  ├── AccountRepository                                      │
│  ├── TransactionRepository                                  │
│  └── ...other repositories                                  │
├─────────────────────────────────────────────────────────────┤
│  Domain Entities (JPA Models)                               │
│  ├── User, Role, Account, Transaction                       │
│  ├── Loan, FixedDeposit, Card, Beneficiary                  │
│  └── SupportTicket                                          │
├─────────────────────────────────────────────────────────────┤
│  Security Layer                                             │
│  ├── SecurityConfig      - Spring Security configuration    │
│  ├── JwtAuthFilter       - JWT request filter               │
│  ├── JwtService          - Token generation/validation      │
│  └── CustomUserDetailsService - User loading                │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
                    ┌───────────┐
                    │  H2/MySQL │
                    │  Database │
                    └───────────┘
```

---

## Project Structure

```
myapp/
├── src/main/java/com/example/
│   └── banking/
│       ├── BankingApplication.java       # Main entry point
│       ├── account/
│       │   ├── Account.java              # Entity
│       │   ├── AccountRepository.java    # Repository
│       │   ├── AccountService.java       # Service
│       │   └── AccountController.java    # Controller
│       ├── admin/
│       │   ├── AdminController.java
│       │   └── AdminService.java
│       ├── beneficiary/
│       │   ├── Beneficiary.java
│       │   ├── BeneficiaryRepository.java
│       │   ├── BeneficiaryService.java
│       │   └── BeneficiaryController.java
│       ├── card/
│       │   ├── Card.java
│       │   ├── CardRepository.java
│       │   ├── CardService.java
│       │   └── CardController.java
│       ├── common/
│       │   ├── AccountStatus.java        # Enum
│       │   ├── AccountType.java          # Enum
│       │   ├── ApiResponse.java          # Response wrapper
│       │   ├── CorsConfig.java           # CORS configuration
│       │   ├── GlobalExceptionHandler.java
│       │   ├── LoanStatus.java           # Enum
│       │   ├── ReferenceNumberGenerator.java
│       │   ├── RoleType.java             # Enum
│       │   ├── SeedDataRunner.java       # Initial data
│       │   ├── TransactionType.java      # Enum
│       │   └── UserUtil.java             # Utility methods
│       ├── fd/
│       │   ├── FixedDeposit.java
│       │   ├── FixedDepositRepository.java
│       │   ├── FixedDepositService.java
│       │   └── FDController.java
│       ├── loan/
│       │   ├── Loan.java
│       │   ├── LoanRepository.java
│       │   ├── LoanService.java
│       │   └── LoanController.java
│       ├── security/
│       │   ├── SecurityConfig.java
│       │   ├── JwtAuthFilter.java
│       │   ├── JwtService.java
│       │   ├── CustomUserDetailsService.java
│       │   ├── AuthController.java
│       │   ├── AuthService.java
│       │   └── ...other security classes
│       ├── support/
│       │   ├── SupportTicket.java
│       │   ├── SupportTicketRepository.java
│       │   ├── SupportTicketService.java
│       │   └── SupportTicketController.java
│       ├── transaction/
│       │   ├── Transaction.java
│       │   ├── TransactionRepository.java
│       │   ├── TransactionService.java
│       │   └── TransactionController.java
│       └── user/
│           ├── User.java
│           ├── Role.java
│           ├── UserRepository.java
│           ├── RoleRepository.java
│           ├── UserService.java
│           └── UserController.java
├── src/main/resources/
│   └── application.properties            # Configuration
└── pom.xml                               # Maven dependencies
```

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.4.3 | Application framework |
| Spring Security | 6.x | Authentication & Authorization |
| Spring Data JPA | 3.x | ORM and data access |
| Hibernate | 6.x | JPA implementation |
| JJWT | 0.12.5 | JWT token handling |
| H2 Database | - | In-memory dev database |
| MySQL | 8.x | Production database |
| Lombok | 1.18.38 | Boilerplate reduction |
| Maven | - | Build management |

---

## Getting Started

### Prerequisites
- Java 21 or higher
- Maven 3.6+ (or use included wrapper)

### Run the Application

```bash
cd myapp

# Using Maven wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Using Maven wrapper (Linux/Mac)
./mvnw spring-boot:run

# Using global Maven
mvn spring-boot:run
```

### Build the Application

```bash
# Using Maven wrapper
.\mvnw.cmd clean package

# Using global Maven
mvn clean package
```

### Run Tests

```bash
.\mvnw.cmd test
```

### Access the Application

- API Base URL: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:bankdb`
  - Username: `sa`
  - Password: (empty)

---

## Domain Entities

### User Entity

```java
@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @JsonIgnore  // Don't serialize password
    private String password;
    
    private String fullName;
    private boolean enabled = true;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    // Getters and setters...
}
```

### Account Entity

```java
@Entity
@Table(name = "accounts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String accountNumber;
    
    @Enumerated(EnumType.STRING)
    private AccountType accountType;  // SAVINGS, CURRENT
    
    private BigDecimal balance;
    
    @Enumerated(EnumType.STRING)
    private AccountStatus status;  // ACTIVE, FROZEN, CLOSED
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    // Getters and setters...
}
```

### Transaction Entity

```java
@Entity
@Table(name = "transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String transactionReference;
    
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;  // DEPOSIT, WITHDRAW, TRANSFER_OUT, etc.
    
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private boolean flagged = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    // Getters and setters...
}
```

### Important: Entity Annotations

All entities with lazy-loaded relationships must include:

```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
```

This prevents serialization errors when Jackson tries to serialize Hibernate proxies.

---

## Repository Layer

### Basic Repository

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser(User user);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserUsername(String username);
}
```

### Custom Queries

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountAccountNumberOrderByTransactionDateDesc(String accountNumber);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = :type")
    BigDecimal sumByTransactionType(@Param("type") TransactionType type);
    
    long countByFlaggedTrue();
}
```

### Repository Patterns

| Pattern | Example |
|---------|---------|
| Find by field | `findByUsername(String username)` |
| Find by nested field | `findByAccountAccountNumber(String accNo)` |
| Count by condition | `countByFlaggedTrue()` |
| Custom query | `@Query("SELECT ...")` |

---

## Service Layer

### Account Service Example

```java
@Service
@Transactional
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<Account> getUserAccounts(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return accountRepository.findByUser(user);
    }
    
    public Account createAccount(String username, AccountType type, BigDecimal initialDeposit) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(type);
        account.setBalance(initialDeposit);
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);
        
        return accountRepository.save(account);
    }
    
    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis();
    }
}
```

### Transaction Service Example

```java
@Service
@Transactional
public class TransactionService {
    
    public Transaction deposit(String accountNumber, BigDecimal amount, String description) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        // Update balance
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        
        // Create transaction record
        Transaction txn = new Transaction();
        txn.setTransactionReference(generateReferenceNumber());
        txn.setTransactionType(TransactionType.DEPOSIT);
        txn.setAmount(amount);
        txn.setDescription(description);
        txn.setTransactionDate(LocalDateTime.now());
        txn.setAccount(account);
        
        return transactionRepository.save(txn);
    }
    
    public Transaction transfer(String fromAccNo, String toAccNo, BigDecimal amount, String description) {
        Account fromAccount = accountRepository.findByAccountNumber(fromAccNo)
            .orElseThrow(() -> new RuntimeException("Source account not found"));
        Account toAccount = accountRepository.findByAccountNumber(toAccNo)
            .orElseThrow(() -> new RuntimeException("Destination account not found"));
        
        // Check balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        
        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        // Create transaction records
        Transaction debit = new Transaction();
        debit.setTransactionType(TransactionType.TRANSFER_OUT);
        debit.setAmount(amount);
        debit.setAccount(fromAccount);
        // ... set other fields
        transactionRepository.save(debit);
        
        Transaction credit = new Transaction();
        credit.setTransactionType(TransactionType.TRANSFER_IN);
        credit.setAmount(amount);
        credit.setAccount(toAccount);
        // ... set other fields
        transactionRepository.save(credit);
        
        return debit;
    }
}
```

---

## Controller Layer

### REST Controller Pattern

```java
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @GetMapping
    public ApiResponse<List<Account>> getAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        List<Account> accounts = accountService.getUserAccounts(principal.getUsername());
        return ApiResponse.success(accounts);
    }
    
    @GetMapping("/{accountNumber}")
    public ApiResponse<Account> getAccount(@PathVariable String accountNumber) {
        Account account = accountService.getAccount(accountNumber);
        return ApiResponse.success(account);
    }
}
```

### API Response Envelope

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
    
    // Getters...
}
```

### Admin Controller

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    @GetMapping("/dashboard")
    public ApiResponse<DashboardStats> getDashboard() {
        DashboardStats stats = adminService.getDashboardStats();
        return ApiResponse.success(stats);
    }
    
    @GetMapping("/customers")
    public ApiResponse<List<User>> getCustomers() {
        List<User> customers = adminService.getAllCustomers();
        return ApiResponse.success(customers);
    }
    
    @PostMapping("/customers/{id}/freeze")
    public ApiResponse<Void> freezeCustomer(@PathVariable Long id) {
        adminService.freezeCustomer(id);
        return ApiResponse.success(null);
    }
}
```

---

## Security Implementation

### Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .userDetailsService(userDetailsService)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### JWT Service

```java
@Service
public class JwtService {
    
    @Value("${app.jwt.secret}")
    private String secretKey;
    
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;
    
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS384)
            .compact();
    }
    
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
    
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }
    
    private Claims extractClaims(String token) {
        return Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### JWT Authentication Filter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtService.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Auth Controller

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestParam String username, @RequestParam String password) {
        LoginResponse response = authService.authenticate(username, password);
        return ApiResponse.success(response);
    }
    
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ApiResponse.success(response);
    }
    
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("server", "SecureBank API");
        data.put("client", request.getRemoteAddr());
        data.put("time", System.currentTimeMillis());
        data.put("version", "1.0.0");
        return ApiResponse.success(data);
    }
}
```

---

## Configuration

### application.properties

```properties
# Server
server.port=8080

# Database (H2)
spring.datasource.url=jdbc:h2:mem:bankdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JWT
app.jwt.secret=THIS_IS_A_256_BIT_SECRET_KEY_FOR_JWT_TOKEN_GENERATION_EXAMPLE
app.jwt.expiration-ms=86400000
app.jwt.refresh-expiration-ms=2592000000

# Account defaults
app.account.savings-min-balance=500.00
app.account.current-min-balance=1000.00

# Loan defaults
app.loan.max-amount=1000000.00
app.loan.interest-rate=10.5
app.loan.max-tenure-months=60

# Fraud detection
app.fraud.max-daily-transactions=10
app.fraud.max-daily-amount=50000.00
app.fraud.max-failed-login-attempts=3
```

### Switching to MySQL

```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/bankdb?useSSL=false&serverTimezone=UTC
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

---

## Database

### H2 Console Access

1. Start the application
2. Navigate to `http://localhost:8080/h2-console`
3. Enter JDBC URL: `jdbc:h2:mem:bankdb`
4. Username: `sa`, Password: (empty)
5. Click Connect

### Schema Overview

```
users
├── id (PK)
├── username (unique)
├── email (unique)
├── password (encrypted)
├── full_name
├── enabled

roles
├── id (PK)
├── name (ROLE_ADMIN, ROLE_CUSTOMER)

user_roles (join table)
├── user_id (FK → users)
├── role_id (FK → roles)

accounts
├── id (PK)
├── account_number (unique)
├── account_type (SAVINGS, CURRENT)
├── balance
├── status (ACTIVE, FROZEN, CLOSED)
├── user_id (FK → users)

transactions
├── id (PK)
├── transaction_reference (unique)
├── transaction_type
├── amount
├── description
├── transaction_date
├── flagged
├── account_id (FK → accounts)

loans
├── id (PK)
├── loan_amount
├── interest_rate
├── tenure_months
├── status (PENDING, APPROVED, REJECTED)
├── user_id (FK → users)

fixed_deposits
├── id (PK)
├── principal_amount
├── interest_rate
├── maturity_date
├── user_id (FK → users)

beneficiaries
├── id (PK)
├── name
├── account_number
├── verified
├── user_id (FK → users)

cards
├── id (PK)
├── card_number
├── card_type (DEBIT, CREDIT)
├── expiry_date
├── account_id (FK → accounts)

support_tickets
├── id (PK)
├── subject
├── description
├── status (OPEN, IN_PROGRESS, CLOSED)
├── user_id (FK → users)
```

---

## API Documentation

### Authentication APIs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login user | No |
| POST | `/api/auth/refresh` | Refresh JWT token | No |
| POST | `/api/auth/verify-login-otp` | Verify MFA code | No |
| GET | `/api/auth/ping` | Health check | No |

### Account APIs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/accounts` | Get user accounts | Yes |
| GET | `/api/accounts/{number}` | Get account details | Yes |

### Transaction APIs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/transactions/{accNo}` | Get transaction history | Yes |
| POST | `/api/transactions/deposit` | Deposit money | Yes |
| POST | `/api/transactions/withdraw` | Withdraw money | Yes |
| POST | `/api/transactions/transfer` | Transfer to account | Yes |
| POST | `/api/transactions/transfer-beneficiary` | Transfer to beneficiary | Yes |

### Admin APIs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/admin/dashboard` | Dashboard statistics | Admin |
| GET | `/api/admin/customers` | List all customers | Admin |
| GET | `/api/admin/accounts` | List all accounts | Admin |
| GET | `/api/admin/flagged-transactions` | Flagged transactions | Admin |
| POST | `/api/admin/customers/{id}/freeze` | Freeze customer | Admin |
| POST | `/api/admin/customers/{id}/unfreeze` | Unfreeze customer | Admin |
| GET | `/api/admin/reports` | Financial reports | Admin |

---

## Best Practices

### 1. Always Use ApiResponse Wrapper

```java
// CORRECT
@GetMapping("/accounts")
public ApiResponse<List<Account>> getAccounts() {
    return ApiResponse.success(accountService.getAccounts());
}

// WRONG - raw response
@GetMapping("/accounts")
public List<Account> getAccounts() {
    return accountService.getAccounts();
}
```

### 2. Use @Transactional for Write Operations

```java
@Service
public class TransactionService {
    
    @Transactional
    public Transaction transfer(...) {
        // Multiple database operations
        // Will rollback if any fails
    }
}
```

### 3. Handle Exceptions Globally

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.error("An unexpected error occurred");
    }
}
```

### 4. Use Enums for Fixed Values

```java
public enum AccountType {
    SAVINGS, CURRENT
}

public enum TransactionType {
    DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN, CREDIT, DEBIT
}
```

### 5. Lazy Loading for Relationships

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

### 6. Ignore Hibernate Properties

```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Account { ... }
```

### 7. Don't Expose Passwords

```java
@JsonIgnore
private String password;
```

---

## Troubleshooting

### Hibernate Serialization Error

**Error:** `ByteBuddyInterceptor` or `LazyInitializationException`

**Solution:**
- Add `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` to entities
- Use `@JsonIgnore` on password fields

### 401 Unauthorized

**Cause:** Missing or invalid JWT token

**Solution:**
- Check Authorization header format: `Bearer <token>`
- Verify token is not expired
- Check JWT secret matches configuration

### CORS Errors

**Solution:** Configure `CorsConfig.java`:
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3002")
            .allowedMethods("*");
    }
}
```

### Database Connection Issues

**H2:**
- Ensure `spring.datasource.url` is correct
- Check H2 console is accessible

**MySQL:**
- Verify MySQL is running
- Check database exists
- Verify credentials

---

## Development Commands

| Command | Description |
|---------|-------------|
| `.\mvnw.cmd spring-boot:run` | Run application |
| `.\mvnw.cmd clean package` | Build JAR |
| `.\mvnw.cmd test` | Run tests |
| `.\mvnw.cmd dependency:tree` | Show dependencies |
