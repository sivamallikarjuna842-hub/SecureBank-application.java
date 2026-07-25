# Frontend Guide - Banking Management System

This guide covers the frontend architecture, components, state management, and development practices.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Core Concepts](#core-concepts)
- [Component Guide](#component-guide)
- [API Integration](#api-integration)
- [Routing & Navigation](#routing--navigation)
- [State Management](#state-management)
- [Styling & Theming](#styling--theming)
- [Best Practices](#best-practices)

---

## Architecture Overview

```
frontend/
├── src/
│   ├── components/         # Reusable UI components
│   │   ├── Layout.jsx      # Main app shell (sidebar + topbar)
│   │   ├── Primitives.jsx  # Base UI primitives
│   │   └── ToastSystem.jsx # Notification system
│   ├── pages/              # Route-level page components
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── Dashboard.jsx
│   │   ├── AdminDashboard.jsx
│   │   ├── Accounts.jsx
│   │   ├── Transactions.jsx
│   │   ├── Beneficiaries.jsx
│   │   ├── Loans.jsx
│   │   ├── FixedDeposits.jsx
│   │   ├── Cards.jsx
│   │   ├── SupportTickets.jsx
│   │   └── ChatBot.jsx
│   ├── theme/              # Theme configuration
│   │   └── ThemeContext.jsx
│   ├── api.js              # API client & endpoint definitions
│   ├── App.jsx             # Root component & routing
│   ├── main.jsx            # Entry point
│   └── index.css           # Global styles
├── index.html
├── vite.config.js
└── package.json
```

---

## Technology Stack

| Library | Version | Purpose |
|---------|---------|---------|
| React | 18.2 | UI framework |
| Vite | 5.x | Build tool & dev server |
| React Router | 6.x | Client-side routing |
| Axios | 1.6 | HTTP client with interceptors |
| React Bootstrap | 2.9 | UI component library |
| Bootstrap | 5.3 | CSS framework |
| Chart.js | 4.x | Charts and graphs |
| Recharts | 3.x | React charting library |
| Framer Motion | 12.x | Animations |
| Lucide React | 1.x | Icon library |
| React Icons | 4.x | Additional icons |

---

## Getting Started

### Installation

```bash
cd frontend
npm install
```

### Development Server

```bash
npm run dev
```

Starts at `http://localhost:5173` (or configured port 3002).

### Build for Production

```bash
npm run build
```

Output in `dist/` directory.

### Preview Production Build

```bash
npm run preview
```

---

## Core Concepts

### 1. API Client Pattern

All API calls go through a centralized axios instance with interceptors.

**File:** `src/api.js`

```javascript
import axios from 'axios';

const API = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
});

// Request interceptor - attach JWT
API.interceptors.request.use((config) => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (user?.accessToken) {
    config.headers.Authorization = `Bearer ${user.accessToken}`;
  }
  return config;
});

// Response interceptor - unwrap envelope
API.interceptors.response.use(
  (res) => {
    // Backend returns: { success, message, data }
    // Interceptor unwraps to: { ...res, data: <inner data> }
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

export default API;
```

### 2. API Endpoint Definitions

```javascript
// Auth
export const login = (username, password) =>
  API.post(`/auth/login?username=${username}&password=${password}`);

export const register = (data) =>
  API.post('/auth/register', data);

// Accounts
export const getAccounts = () =>
  API.get('/accounts');

// Transactions
export const getTransactions = (accNo) =>
  API.get(`/transactions/${accNo}`);

export const deposit = (accNo, amount, description) =>
  API.post('/transactions/deposit', { accountNumber: accNo, amount, description });

// ... more endpoints
```

### 3. Response Data Access

After the interceptor, response data is accessed via `res.data`:

```javascript
// Backend returns:
// { "success": true, "message": "...", "data": { "accessToken": "...", ... } }

// After interceptor:
const res = await login(username, password);
const data = res.data;  // This is the inner data object
// data.accessToken, data.username, etc.
```

---

## Component Guide

### Layout Component

**File:** `src/components/Layout.jsx`

The main application shell with sidebar navigation and top bar.

```javascript
function Layout({ user, onLogout }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  
  return (
    <div className="sb-shell">
      <Sidebar user={user} onLogout={onLogout} isOpen={sidebarOpen} />
      <div className="sb-main">
        <TopBar user={user} onMenuClick={() => setSidebarOpen(true)} onLogout={onLogout} />
        <main className="sb-content">
          <Outlet />  {/* Nested routes */}
        </main>
      </div>
    </div>
  );
}
```

**Key Props:**
- `user` - Current user object from localStorage
- `onLogout` - Logout handler passed from App

### Page Component Pattern

All page components follow a consistent pattern:

```javascript
function PageName() {
  // 1. State declarations
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // 2. Data fetching
  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await apiCall();
        setData(res.data);  // Note: res.data after interceptor
      } catch (err) {
        setError('Failed to load data.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);
  
  // 3. Event handlers
  const handleAction = async () => { ... };
  
  // 4. Loading state
  if (loading) return <Spinner />;
  
  // 5. Main render
  return (
    <div>
      {error && <Alert variant="danger">{error}</Alert>}
      {/* Page content */}
    </div>
  );
}
```

### Dashboard Component

**File:** `src/pages/Dashboard.jsx`

Customer dashboard showing accounts, transactions, loans, etc.

**Key Features:**
- Account balance cards
- Recent transactions (mini statement)
- Quick action buttons
- Loan and FD overview

### AdminDashboard Component

**File:** `src/pages/AdminDashboard.jsx`

Admin control panel with statistics and management tools.

**Tabs:**
- Overview - Statistics cards
- Customers - Customer list with freeze/unfreeze
- Accounts - All accounts overview
- Flagged - Suspicious transactions
- Reports - Financial reports

---

## API Integration

### Making API Calls

```javascript
import { getAccounts, deposit } from '../api';

function Accounts() {
  const [accounts, setAccounts] = useState([]);
  
  useEffect(() => {
    const fetchAccounts = async () => {
      const res = await getAccounts();
      setAccounts(res.data);  // Array of accounts
    };
    fetchAccounts();
  }, []);
  
  const handleDeposit = async (accNo, amount) => {
    await deposit(accNo, amount, 'Deposit description');
    // Refresh accounts
    const res = await getAccounts();
    setAccounts(res.data);
  };
}
```

### Error Handling

```javascript
try {
  const res = await someApiCall();
  setData(res.data);
} catch (err) {
  // err.response contains the error response
  const message = err.response?.data?.message || 'An error occurred';
  setError(message);
}
```

### File Upload (Future)

```javascript
const uploadDocument = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const res = await API.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
};
```

---

## Routing & Navigation

### Route Configuration

**File:** `src/App.jsx`

```javascript
function App() {
  const [user, setUser] = useState(null);
  
  // Role-aware default route
  const defaultRoute = user?.roles?.includes('ROLE_ADMIN') ? '/admin' : '/dashboard';
  
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={
        user ? <Navigate to={defaultRoute} /> : <Login onLogin={handleLogin} />
      } />
      <Route path="/register" element={
        user ? <Navigate to={defaultRoute} /> : <Register />
      } />
      
      {/* Protected routes */}
      <Route element={user ? <Layout user={user} onLogout={handleLogout} /> : <Navigate to="/login" />}>
        <Route path="/dashboard" element={<Dashboard user={user} />} />
        <Route path="/accounts" element={<Accounts />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/beneficiaries" element={<Beneficiaries />} />
        <Route path="/loans" element={<Loans />} />
        <Route path="/fixed-deposits" element={<FixedDeposits />} />
        <Route path="/cards" element={<Cards />} />
        <Route path="/support" element={<SupportTickets />} />
        <Route path="/chatbot" element={<ChatBot />} />
        <Route path="/admin" element={
          user?.roles?.includes('ROLE_ADMIN') ? <AdminDashboard /> : <Navigate to="/dashboard" />
        } />
      </Route>
      
      {/* Catch-all */}
      <Route path="*" element={<Navigate to={user ? defaultRoute : '/login'} />} />
    </Routes>
  );
}
```

### Navigation

```javascript
import { useNavigate } from 'react-router-dom';

function MyComponent() {
  const navigate = useNavigate();
  
  const handleClick = () => {
    navigate('/transactions');
  };
  
  return <button onClick={handleClick}>View Transactions</button>;
}
```

---

## State Management

### Local Component State

```javascript
const [data, setData] = useState([]);
const [loading, setLoading] = useState(true);
const [error, setError] = useState('');
```

### Global User State

User state is managed in `App.jsx` and passed down:

```javascript
// App.jsx
const [user, setUser] = useState(null);

const handleLogin = (userData) => {
  setUser(userData);
  localStorage.setItem('user', JSON.stringify(userData));
};

// Passed to Layout, Dashboard, etc.
<Layout user={user} onLogout={handleLogout} />
```

### Theme State

```javascript
// ThemeContext.jsx
const ThemeContext = createContext();

export function ThemeProvider({ children }) {
  const [mode, setMode] = useState('light');
  
  const toggle = () => {
    setMode(m => m === 'light' ? 'dark' : 'light');
  };
  
  return (
    <ThemeContext.Provider value={{ mode, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

// Usage
const { mode, toggle } = useThemeMode();
```

---

## Styling & Theming

### Bootstrap Integration

```javascript
// main.jsx
import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css';
```

### Custom CSS

**File:** `src/index.css`

```css
/* Global styles */
:root {
  --primary-color: #0d6efd;
  --bg-color: #f8f9fa;
}

/* Dark mode */
[data-bs-theme="dark"] {
  --bg-color: #212529;
}

/* Component styles */
.dashboard-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
```

### Theme Toggle

```javascript
function TopBar() {
  const { mode, toggle } = useThemeMode();
  
  useEffect(() => {
    document.documentElement.setAttribute('data-bs-theme', mode);
  }, [mode]);
  
  return (
    <button onClick={toggle}>
      {mode === 'light' ? '🌙' : '☀️'}
    </button>
  );
}
```

---

## Best Practices

### 1. Always Use `res.data` After Interceptor

```javascript
// CORRECT
const res = await apiCall();
const data = res.data;

// WRONG - data is already unwrapped
const data = await apiCall();  // This is the full response
```

### 2. Handle Loading and Error States

```javascript
if (loading) return <Spinner />;
if (error) return <Alert variant="danger">{error}</Alert>;
return <MainContent />;
```

### 3. Clean Up localStorage on Logout

```javascript
const handleLogout = () => {
  setUser(null);
  localStorage.clear();  // Clear all stored data
  navigate('/login');
};
```

### 4. Use Optional Chaining for Nested Data

```javascript
// Safe access
const name = user?.fullName || 'Guest';
const role = user?.roles?.[0] || 'UNKNOWN';
```

### 5. Consistent Error Messages

```javascript
catch (err) {
  const message = err.response?.data?.message || 'Operation failed. Please try again.';
  setError(message);
}
```

### 6. Memoize Expensive Computations

```javascript
import { useMemo } from 'react';

const totalBalance = useMemo(() => {
  return accounts.reduce((sum, acc) => sum + acc.balance, 0);
}, [accounts]);
```

### 7. Debounce Search Inputs

```javascript
import { debounce } from 'lodash';

const handleSearch = debounce((query) => {
  fetchResults(query);
}, 300);
```

---

## Common Patterns

### Data Fetching Pattern

```javascript
useEffect(() => {
  let cancelled = false;
  
  const fetchData = async () => {
    try {
      const res = await apiCall();
      if (!cancelled) {
        setData(res.data);
      }
    } catch (err) {
      if (!cancelled) {
        setError(err.message);
      }
    }
  };
  
  fetchData();
  
  return () => { cancelled = true; };
}, [dependency]);
```

### Form Submission Pattern

```javascript
const [submitting, setSubmitting] = useState(false);

const handleSubmit = async (e) => {
  e.preventDefault();
  setSubmitting(true);
  setError('');
  
  try {
    await apiCall(formData);
    setShowModal(false);
    refreshData();
  } catch (err) {
    setError(err.response?.data?.message || 'Submission failed');
  } finally {
    setSubmitting(false);
  }
};
```

### Modal Pattern

```javascript
const [showModal, setShowModal] = useState(false);
const [formData, setFormData] = useState({ field1: '', field2: '' });

return (
  <>
    <Button onClick={() => setShowModal(true)}>Open</Button>
    
    <Modal show={showModal} onHide={() => setShowModal(false)}>
      <Modal.Header closeButton>
        <Modal.Title>Title</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form>
          <Form.Control
            value={formData.field1}
            onChange={(e) => setFormData({...formData, field1: e.target.value})}
          />
        </Form>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
        <Button variant="primary" onClick={handleSubmit} disabled={submitting}>
          {submitting ? 'Processing...' : 'Submit'}
        </Button>
      </Modal.Footer>
    </Modal>
  </>
);
```

---

## Troubleshooting

### Blank Page After Login

**Cause:** Missing `res.data` extraction.

**Fix:**
```javascript
const res = await login(...);
const data = res.data;  // Add this line
```

### 401 Unauthorized Errors

**Cause:** JWT token not being sent.

**Fix:** Check localStorage has user data with accessToken.

### CORS Errors

**Cause:** Backend not configured to allow frontend origin.

**Fix:** Update `CorsConfig.java` on backend.

### Component Not Re-rendering

**Cause:** Mutating state directly.

**Fix:**
```javascript
// WRONG
data.push(newItem);
setData(data);

// CORRECT
setData([...data, newItem]);
```

---

## Development Commands

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |
| `npm install <package>` | Add new dependency |
