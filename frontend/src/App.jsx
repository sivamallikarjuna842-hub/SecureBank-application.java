import { Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { startHealthMonitor } from './api';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Accounts from './pages/Accounts';
import Transactions from './pages/Transactions';
import Beneficiaries from './pages/Beneficiaries';
import Loans from './pages/Loans';
import FixedDeposits from './pages/FixedDeposits';
import Cards from './pages/Cards';
import SupportTickets from './pages/SupportTickets';
import ChatBot from './pages/ChatBot';
import AdminDashboard from './pages/AdminDashboard';
import Layout from './components/Layout';

function App() {
  const [user, setUser] = useState(null);
  const location = useLocation();
  const navigate = useNavigate();

  // Start backend health monitor as soon as the app boots
  useEffect(() => { startHealthMonitor(20000); }, []);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try { setUser(JSON.parse(storedUser)); } catch { /* ignore */ }
    }
  }, []);

  // Apply login-page class to body for the gradient backdrop on /login and /register
  useEffect(() => {
    const isLoginPage = location.pathname === '/login' || location.pathname === '/register';
    document.body.classList.toggle('login-page', isLoginPage);
    return () => document.body.classList.remove('login-page');
  }, [location.pathname]);

  const handleLogin = (userData) => {
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.clear();
    navigate('/login');
  };

  // Role-aware default redirect: admins go to /admin, customers to /dashboard
  const defaultRoute = user?.roles?.includes('ROLE_ADMIN') ? '/admin' : '/dashboard';

  return (
    <div className="App">
      <Routes>
        {/* Public auth routes (no sidebar/topbar) */}
        <Route path="/login"    element={user ? <Navigate to={defaultRoute} replace /> : <Login onLogin={handleLogin} />} />
        <Route path="/register" element={user ? <Navigate to={defaultRoute} replace /> : <Register />} />

        {/* Authenticated app routes wrapped in the new Layout (sidebar + topbar) */}
        <Route element={user ? <Layout user={user} onLogout={handleLogout} /> : <Navigate to="/login" replace />}>
          <Route path="/dashboard"        element={<Dashboard user={user} />} />
          <Route path="/accounts"         element={<Accounts />} />
          <Route path="/transactions"     element={<Transactions />} />
          <Route path="/beneficiaries"    element={<Beneficiaries />} />
          <Route path="/loans"            element={<Loans />} />
          <Route path="/fixed-deposits"   element={<FixedDeposits />} />
          <Route path="/cards"            element={<Cards />} />
          <Route path="/support"          element={<SupportTickets />} />
          <Route path="/chatbot"          element={<ChatBot />} />
          <Route path="/admin"            element={user?.roles?.includes('ROLE_ADMIN') ? <AdminDashboard /> : <Navigate to="/dashboard" replace />} />
        </Route>

        <Route path="/"  element={<Navigate to={user ? defaultRoute : '/login'} replace />} />
        <Route path="*"  element={<Navigate to={user ? defaultRoute : '/login'} replace />} />
      </Routes>
    </div>
  );
}

export default App;
