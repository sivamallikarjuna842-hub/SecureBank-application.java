# Admin Guide - Banking Management System

This guide covers all administrative features and operations available in the Banking Management System.

## Table of Contents

- [Accessing Admin Dashboard](#accessing-admin-dashboard)
- [Dashboard Overview](#dashboard-overview)
- [Customer Management](#customer-management)
- [Account Management](#account-management)
- [Flagged Transactions](#flagged-transactions)
- [Reports](#reports)
- [Admin API Reference](#admin-api-reference)

---

## Accessing Admin Dashboard

### Login
1. Navigate to `http://localhost:3002/login` (or your deployed URL)
2. Enter admin credentials:
   - **Username:** `admin`
   - **Password:** `admin123`
3. Click **Sign In**
4. You will be automatically redirected to `/admin`

### Admin Authentication
- Admin users are identified by the `ROLE_ADMIN` role in the JWT token
- The frontend routes admins to `/admin` automatically based on their role
- Admin session is stored in localStorage and persists across page refreshes

### Logout
- Click the profile avatar (top-right corner)
- Select **Sign out** from the dropdown menu
- You will be redirected to the login page

---

## Dashboard Overview

The admin dashboard provides a comprehensive view of the banking system's status.

### Statistics Cards

| Card | Description |
|------|-------------|
| **Total Users** | Number of registered customers in the system |
| **Total Accounts** | Number of active bank accounts |
| **Pending Loans** | Loan applications awaiting approval |
| **Open Tickets** | Unresolved customer support tickets |

### Navigation Tabs

| Tab | Purpose |
|-----|---------|
| **Overview** | Main dashboard with statistics |
| **Customers** | Customer list and management |
| **Accounts** | All bank accounts overview |
| **Flagged** | Suspicious transactions for review |
| **Reports** | Financial reports and analytics |

---

## Customer Management

### View All Customers

Navigate to the **Customers** tab to see a list of all registered customers.

**Customer Information Displayed:**
- Customer ID
- Full Name
- Email Address
- Account Status (Active/Frozen)
- Registration Date

### Freeze Customer Account

Freezing a customer account prevents them from performing any transactions.

**Steps:**
1. Go to **Customers** tab
2. Find the customer in the list
3. Click the **Freeze** button next to their name
4. Confirm the action

**API Endpoint:**
```
POST /api/admin/customers/{id}/freeze
```

### Unfreeze Customer Account

Unfreezing restores the customer's ability to transact.

**Steps:**
1. Go to **Customers** tab
2. Find the frozen customer (status shows as "Frozen")
3. Click the **Unfreeze** button
4. Confirm the action

**API Endpoint:**
```
POST /api/admin/customers/{id}/unfreeze
```

### Customer Account Status

| Status | Description | Can Transact? |
|--------|-------------|---------------|
| **Active** | Normal operating status | Yes |
| **Frozen** | Account suspended by admin | No |
| **Locked** | Too many failed login attempts | No (temporary) |

---

## Account Management

### View All Accounts

Navigate to the **Accounts** tab to see all bank accounts.

**Account Information Displayed:**
- Account Number
- Account Type (Savings/Current)
- Owner Name
- Current Balance
- Status

### Account Types

| Type | Minimum Balance | Features |
|------|-----------------|----------|
| **Savings** | $500 | Interest earning, transaction limits |
| **Current** | $1,000 | Business use, higher transaction limits |

---

## Flagged Transactions

The fraud detection system automatically flags suspicious transactions based on configurable thresholds.

### Flagging Criteria

Transactions are flagged when:
- Daily transaction count exceeds `app.fraud.max-daily-transactions` (default: 10)
- Daily transaction amount exceeds `app.fraud.max-daily-amount` (default: $50,000)
- Multiple failed login attempts detected

### Review Flagged Transactions

1. Navigate to the **Flagged** tab
2. Review the list of flagged transactions
3. Each flagged transaction shows:
   - Transaction ID
   - Account number
   - Transaction type and amount
   - Reason for flagging
   - Timestamp

### Transaction Actions

| Action | Description |
|--------|-------------|
| **Approve** | Mark as legitimate, remove flag |
| **Reject** | Mark as fraudulent, reverse if needed |
| **Investigate** | Request more information from customer |

---

## Reports

The **Reports** tab provides financial analytics and system statistics.

### Available Reports

| Report | Contents |
|--------|----------|
| **Financial Summary** | Total deposits, withdrawals, transfers |
| **Account Statistics** | Accounts by type, balance distribution |
| **Loan Report** | Approved, pending, rejected loans |
| **Transaction Volume** | Daily/monthly transaction counts |

### Report Data Fields

| Field | Description |
|-------|-------------|
| `totalDeposits` | Sum of all deposit transactions |
| `totalWithdrawals` | Sum of all withdrawal transactions |
| `totalTransfers` | Sum of all transfer transactions |
| `totalLoansApproved` | Number of approved loans |
| `totalLoansPending` | Number of pending loan applications |

---

## Admin API Reference

### Authentication

#### Login as Admin
```http
POST /api/auth/login?username=admin&password=admin123
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4...",
    "username": "admin",
    "userId": 1,
    "role": "ROLE_ADMIN",
    "expiresIn": 86400,
    "refreshToken": "eyJhbGciOiJIUzM4...",
    "tokenType": "Bearer",
    "email": "admin@bank.com",
    "mfaEnabled": false,
    "fullName": "Admin User"
  }
}
```

### Dashboard APIs

#### Get Dashboard Statistics
```http
GET /api/admin/dashboard
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalUsers": 3,
    "totalAccounts": 3,
    "pendingLoans": 1,
    "openTickets": 0
  }
}
```

#### Get All Customers
```http
GET /api/admin/customers
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 2,
      "username": "john",
      "email": "john@example.com",
      "fullName": "John Doe",
      "enabled": true,
      "roles": ["ROLE_CUSTOMER"]
    }
  ]
}
```

#### Get All Accounts
```http
GET /api/admin/accounts
Authorization: Bearer {accessToken}
```

#### Get Flagged Transactions
```http
GET /api/admin/flagged-transactions
Authorization: Bearer {accessToken}
```

#### Freeze Customer
```http
POST /api/admin/customers/{id}/freeze
Authorization: Bearer {accessToken}
```

#### Unfreeze Customer
```http
POST /api/admin/customers/{id}/unfreeze
Authorization: Bearer {accessToken}
```

#### Get Reports
```http
GET /api/admin/reports
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalDeposits": 150000.00,
    "totalWithdrawals": 25000.00,
    "totalTransfers": 10000.00,
    "totalLoansApproved": 2,
    "totalLoansPending": 1
  }
}
```

---

## Security Notes

### Admin Access Control
- Only users with `ROLE_ADMIN` can access admin endpoints
- Spring Security enforces role-based access at the backend level
- The frontend also validates admin role before rendering admin components

### JWT Token Handling
- Access tokens expire after 24 hours (configurable in `application.properties`)
- Refresh tokens expire after 30 days
- Tokens are stored in localStorage on the frontend
- Always include the Bearer token in the Authorization header

### Audit Trail
- All admin actions should be logged for audit purposes
- Transaction flags include timestamps and reasons
- Customer freeze/unfreeze actions are recorded

---

## Troubleshooting

### Cannot Access Admin Dashboard

**Problem:** Login succeeds but redirects to customer dashboard instead of admin.

**Solution:**
- Verify the user has `ROLE_ADMIN` role in the database
- Clear localStorage and re-login
- Check browser console for errors

### API Returns 403 Forbidden

**Problem:** Admin API calls return 403.

**Solution:**
- Verify JWT token is valid and not expired
- Check that the token contains `ROLE_ADMIN`
- Re-login to get a fresh token

### Customers Not Loading

**Problem:** Customer list is empty or shows error.

**Solution:**
- Check backend logs for database connection issues
- Verify `/api/admin/customers` endpoint is accessible
- Check browser network tab for failed requests

---

## Configuration

### Admin-Related Properties

Edit `application.properties` to customize admin settings:

```properties
# Fraud detection thresholds
app.fraud.max-daily-transactions=10
app.fraud.max-daily-amount=50000.00
app.fraud.max-failed-login-attempts=3

# JWT expiration (milliseconds)
app.jwt.expiration-ms=86400000
```
