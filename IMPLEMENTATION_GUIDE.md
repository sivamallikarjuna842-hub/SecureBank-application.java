# Implementation Guide - Banking Management System

This document provides a step-by-step guide to the development process, architecture decisions, and implementation details of the Banking Management System.

## Table of Contents

- [Development Process Overview](#development-process-overview)
- [Phase 1: Project Setup](#phase-1-project-setup)
- [Phase 2: Backend Development](#phase-2-backend-development)
- [Phase 3: Frontend Development](#phase-3-frontend-development)
- [Phase 4: Integration & Testing](#phase-4-integration--testing)
- [Phase 5: Bug Fixes & Refinements](#phase-5-bug-fixes--refinements)
- [Key Implementation Decisions](#key-implementation-decisions)
- [Common Issues & Solutions](#common-issues--solutions)

---

## Development Process Overview

```
Phase 1: Project Setup
├── Backend: Spring Boot initialization
├── Frontend: React + Vite setup
└── Database: H2 configuration

Phase 2: Backend Development
├── Domain entities (JPA models)
├── Repository layer
├── Service layer
├── Controller layer (REST APIs)
└── Security (JWT + Spring Security)

Phase 3: Frontend Development
├── API client setup
├── Authentication flow
├── Page components
├── Routing & navigation
└── UI/UX refinement

Phase 4: Integration & Testing
├── Frontend-backend connection
├── JWT authentication flow
├── Role-based access control
└── End-to-end testing

Phase 5: Bug Fixes & Refinements
├── Serialization issues
├── Data access patterns
├── Routing fixes
└── Component bug fixes
```

---

## Phase 1: Project Setup

### Step 1.1: Backend Initialization

**Tool:** Spring Initializr (https://start.spring.io)

**Configuration:**
- Project: Maven
- Language: Java
- Spring Boot: 3.4.3
- Java Version: 21

**Dependencies Selected:**
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Mail
- H2 Database
- MySQL Driver
- Lombok
- DevTools

**Post-Setup:**
```xml
<!-- Add JWT dependencies manually to pom.xml -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
```

### Step 1.2: Frontend Initialization

**Tool:** Vite CLI

```bash
npm create vite@latest frontend -- --template react
cd frontend
npm install
```

**Additional Dependencies:**
```bash
npm install react-router-dom axios react-bootstrap bootstrap
npm install chart.js react-chartjs-2 recharts
npm install framer-motion lucide-react react-icons
```

### Step 1.3: Database Configuration

**File:** `application.properties`

```properties
# H2 In-Memory Database (Development)
spring.datasource.url=jdbc:h2:mem:bankdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# H2 Console (for debugging)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### Step 1.4: Seed Data

**File:** `SeedDataRunner.java`

Creates initial data on application startup:
- Admin user: `admin` / `admin123`
- Customer users: `john` / `john123`, `jane` / `jane123`
- Sample accounts, transactions, loans

---

## Phase 2: Backend Development

### Step 2.1: Domain Entities

**Order of Implementation:**
1. `User.java` - Core user entity
2. `Role.java` - User roles (ADMIN, CUSTOMER)
3. `Account.java` - Bank accounts
4. `Transaction.java` - Transaction records
5. `Beneficiary.java` - Beneficiary management
6. `Loan.java` - Loan applications
7. `FixedDeposit.java` - Fixed deposits
8. `Card.java` - Debit/Credit cards
9. `SupportTicket.java` - Support system

**Key Design Decisions:**
- Use `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` on all entities to prevent serialization errors
- Use `FetchType.LAZY` for `@ManyToOne` relationships
- Use enums for fixed values (AccountType, TransactionType, LoanStatus)

**Example Entity:**
```java
@Entity
@Table(name = "accounts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String accountNumber;
    
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    private BigDecimal balance;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    // Getters and setters...
}
```

### Step 2.2: Repository Layer

**Pattern:** Spring Data JPA repositories

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser(User user);
    Optional<Account> findByAccountNumber(String accountNumber);
}
```

### Step 2.3: Service Layer

**Pattern:** Business logic encapsulation

**Key Services:**
- `AuthService` - Registration, login, token management
- `AccountService` - Account operations
- `TransactionService` - Deposit, withdraw, transfer
- `AdminService` - Admin-specific operations

**Example:**
```java
@Service
public class TransactionService {
    public Transaction deposit(String accountNumber, BigDecimal amount, String description) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        
        Transaction txn = new Transaction();
        txn.setAccount(account);
        txn.setAmount(amount);
        txn.setTransactionType(TransactionType.DEPOSIT);
        // ... set other fields
        return transactionRepository.save(txn);
    }
}
```

### Step 2.4: Controller Layer

**Pattern:** RESTful API design

**Key Controllers:**
- `AuthController` - `/api/auth/*`
- `AccountController` - `/api/accounts/*`
- `TransactionController` - `/api/transactions/*`
- `AdminController` - `/api/admin/*`

**Response Envelope:**
```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    // Static factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data);
    }
}
```

### Step 2.5: Security Implementation

**Components:**
1. `JwtService` - Token generation and validation
2. `JwtAuthFilter` - Request authentication
3. `SecurityConfig` - Security configuration
4. `CustomUserDetailsService` - User loading

**Security Flow:**
```
1. User submits credentials → AuthController
2. AuthService validates → AuthenticationManager
3. On success → JwtService generates tokens
4. Tokens returned to client
5. Client includes Bearer token in subsequent requests
6. JwtAuthFilter validates token on each request
7. SecurityContext populated with user details
```

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

## Phase 3: Frontend Development

### Step 3.1: API Client Setup

**File:** `api.js`

```javascript
import axios from 'axios';

const API = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
});

// Request interceptor - attach JWT token
API.interceptors.request.use((config) => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (user?.accessToken) {
    config.headers.Authorization = `Bearer ${user.accessToken}`;
  }
  return config;
});

// Response interceptor - unwrap ApiResponse envelope
API.interceptors.response.use(
  (res) => {
    if (res.data && 'success' in res.data && 'data' in res.data) {
      return { ...res, data: res.data.data };
    }
    return res;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### Step 3.2: Authentication Flow

**Login Component:**
```javascript
const handleLogin = async (username, password) => {
  const res = await login(username, password);
  const data = res.data;  // Unwrapped by interceptor
  
  if (data?.accessToken) {
    // Store user data
    setUser(data);
    localStorage.setItem('user', JSON.stringify(data));
    
    // Route based on role
    if (data.roles?.includes('ROLE_ADMIN')) {
      navigate('/admin');
    } else {
      navigate('/dashboard');
    }
  }
};
```

### Step 3.3: Routing Setup

**File:** `App.jsx`

```javascript
function App() {
  const [user, setUser] = useState(null);
  
  // Role-aware default route
  const defaultRoute = user?.roles?.includes('ROLE_ADMIN') ? '/admin' : '/dashboard';
  
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={defaultRoute} /> : <Login />} />
      
      <Route element={user ? <Layout /> : <Navigate to="/login" />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/admin" element={
          user?.roles?.includes('ROLE_ADMIN') ? <AdminDashboard /> : <Navigate to="/dashboard" />
        } />
        {/* Other routes... */}
      </Route>
    </Routes>
  );
}
```

### Step 3.4: Page Components

**Component Structure:**
```
Page Component
├── State declarations (useState)
├── Data fetching (useEffect)
├── Event handlers
├── Render logic
│   ├── Loading state
│   ├── Error state
│   └── Main content
└── Modal/Form components
```

**Example - Dashboard:**
```javascript
function Dashboard({ user }) {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const fetchData = async () => {
      const res = await getAccounts();
      setAccounts(res.data);  // Note: res.data after interceptor
      setLoading(false);
    };
    fetchData();
  }, []);
  
  if (loading) return <Spinner />;
  
  return (
    <div>
      <h3>Welcome, {user?.fullName}</h3>
      {/* Account cards, transactions, etc. */}
    </div>
  );
}
```

### Step 3.5: Layout & Navigation

**File:** `Layout.jsx`

```javascript
function Layout({ user, onLogout }) {
  return (
    <div className="app-container">
      <Sidebar user={user} onLogout={onLogout} />
      <div className="main-content">
        <TopBar user={user} onLogout={onLogout} />
        <Outlet />  {/* Nested routes render here */}
      </div>
    </div>
  );
}
```

---

## Phase 4: Integration & Testing

### Step 4.1: Frontend-Backend Connection

**Vite Proxy Configuration:**
```javascript
// vite.config.js
export default defineConfig({
  server: {
    port: 3002,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
});
```

### Step 4.2: JWT Authentication Testing

**Test Flow:**
1. Start backend: `cd myapp && mvnw spring-boot:run`
2. Start frontend: `cd frontend && npm run dev`
3. Navigate to `http://localhost:3002/login`
4. Login with `john` / `john123`
5. Verify redirect to `/dashboard`
6. Check localStorage for user data
7. Verify API calls include Authorization header

### Step 4.3: Role-Based Access Testing

**Admin Access:**
1. Login with `admin` / `admin123`
2. Verify redirect to `/admin`
3. Verify admin APIs return data
4. Verify customer cannot access `/admin`

---

## Phase 5: Bug Fixes & Refinements

### Bug 1: Hibernate Serialization Error

**Problem:** `ByteBuddyInterceptor` error when serializing entities with lazy-loaded relationships.

**Solution:** Add `@JsonIgnoreProperties` to all entities:
```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Account { ... }
```

### Bug 2: Login Not Redirecting

**Problem:** Login succeeds but doesn't navigate to dashboard.

**Root Cause:** API interceptor unwraps response, so `data.accessToken` is at `res.data.accessToken`, not `data.accessToken`.

**Solution:**
```javascript
// Before (broken):
const data = await login(username, password);
if (data?.accessToken) { ... }

// After (fixed):
const res = await login(username, password);
const data = res.data;
if (data?.accessToken) { ... }
```

### Bug 3: Admin Dashboard Field Mismatches

**Problem:** Frontend references non-existent backend fields.

**Solution:** Align frontend with backend response:
- `c.frozen` → `c.enabled` (inverted logic)
- `dashboard.totalBalance` → removed (not provided)
- `report.totalDeposit` → `report.totalDeposits`

### Bug 4: Logout Crash

**Problem:** Clicking "Sign out" crashes the app.

**Root Cause:** `TopBar` component doesn't receive `onLogout` prop.

**Solution:**
```javascript
// Layout.jsx
function TopBar({ user, onMenuClick, onLogout }) { ... }

// Pass prop:
<TopBar user={user} onMenuClick={...} onLogout={onLogout} />
```

### Bug 5: Transactions Page Crash

**Problem:** Transactions page shows blank screen.

**Root Cause:** Missing `txnLoading` state and variable typo (`tnx` vs `txn`).

**Solution:**
```javascript
// Add missing state:
const [txnLoading, setTxnLoading] = useState(false);

// Fix variable names:
{transactions.map(txn => (
  <tr key={txn.id}>  // was tnx.id
    <td>{txn.transactionReference}</td>  // was tnx.referenceNumber
  </tr>
))}
```

### Bug 6: Admin Routing Issue

**Problem:** Admin users redirected to `/dashboard` instead of `/admin`.

**Solution:** Role-aware routing:
```javascript
const defaultRoute = user?.roles?.includes('ROLE_ADMIN') ? '/admin' : '/dashboard';
```

---

## Key Implementation Decisions

### 1. API Response Envelope

**Decision:** Wrap all responses in `ApiResponse<T>` with `{success, message, data}`.

**Rationale:**
- Consistent error handling
- Clear success/failure indication
- Easy to add metadata

### 2. JWT Storage

**Decision:** Store tokens in localStorage.

**Trade-offs:**
- Pros: Simple, persists across tabs
- Cons: Vulnerable to XSS (consider httpOnly cookies for production)

### 3. H2 for Development

**Decision:** Use H2 in-memory database for development.

**Rationale:**
- Zero configuration
- Fast startup
- Easy to reset state
- Can switch to MySQL for production

### 4. Vite Proxy

**Decision:** Use Vite dev server proxy for API calls.

**Rationale:**
- Avoids CORS issues in development
- Simpler configuration
- No need for absolute URLs

---

## Common Issues & Solutions

### Issue: CORS Errors

**Solution:** Backend `CorsConfig.java` allows frontend origin:
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3002", "http://localhost:5173")
            .allowedMethods("*")
            .allowedHeaders("*");
    }
}
```

### Issue: 401 Unauthorized on API Calls

**Solution:** Ensure JWT token is included in Authorization header:
```javascript
API.interceptors.request.use((config) => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (user?.accessToken) {
    config.headers.Authorization = `Bearer ${user.accessToken}`;
  }
  return config;
});
```

### Issue: Entity Serialization Errors

**Solution:** Add `@JsonIgnoreProperties` to entities with lazy relationships:
```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
```

### Issue: Frontend Not Navigating After Login

**Solution:** Check API response structure after interceptor:
```javascript
const res = await login(...);
const data = res.data;  // Interceptor unwraps to res.data
```

---

## Next Steps & Future Enhancements

1. **Production Database:** Switch from H2 to MySQL/PostgreSQL
2. **Email Integration:** Enable password reset emails
3. **MFA Enforcement:** Make MFA mandatory for all users
4. **Audit Logging:** Track all admin actions
5. **Rate Limiting:** Implement API rate limiting
6. **File Uploads:** Add document upload for loan applications
7. **Notifications:** Real-time notifications using WebSockets
8. **Internationalization:** Multi-language support
9. **Mobile App:** React Native mobile application
10. **Analytics Dashboard:** Advanced reporting with charts
