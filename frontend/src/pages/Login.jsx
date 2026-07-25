import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Landmark, LogIn, Loader2, ShieldCheck } from 'lucide-react';
import { login, verifyLoginOtp, tokenStore } from '../api';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [mfaStep, setMfaStep] = useState(false);
  const [mfaCode, setMfaCode] = useState('');
  const [mfaHint, setMfaHint] = useState('');
  const navigate = useNavigate();

  const finalizeSession = (data) => {
    tokenStore.set({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: {
        username: data.username,
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        roles: data.role === 'ROLE_ADMIN' ? ['ROLE_ADMIN'] : ['ROLE_CUSTOMER'],
      },
    });
    onLogin({
      username: data.username,
      fullName: data.fullName || data.username,
      email: data.email,
      roles: data.role === 'ROLE_ADMIN' ? ['ROLE_ADMIN'] : ['ROLE_CUSTOMER'],
      role: data.role,
    });
    navigate(data.role === 'ROLE_ADMIN' ? '/admin' : '/dashboard');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await login(username, password);
      const data = res.data;
      if (data?.mfaRequired) {
        setMfaStep(true);
        setMfaHint(
          'Code sent via ' + (data.channel || 'EMAIL') +
          (data.devCode ? ' (dev: ' + data.devCode + ')' : '')
        );
      } else if (data?.accessToken) {
        finalizeSession(data);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleMfaSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await verifyLoginOtp(username, mfaCode);
      const data = res.data;
      if (data?.accessToken) finalizeSession(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid verification code.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="sb-auth-wrap">
      <motion.div
        className="sb-auth-card"
        initial={{ opacity: 0, y: 20, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
      >
        <div className="sb-auth-brand">
          <div className="sb-auth-brand-icon"><Landmark size={26} /></div>
          <div className="sb-auth-brand-text">SecureBank</div>
        </div>

        <h2 style={{ textAlign: 'center' }}>Welcome back</h2>
        <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.85)', marginBottom: 24, fontSize: '0.9rem' }}>
          Sign in to continue to your dashboard
        </p>

        {error && <div className="sb-auth-error">{error}</div>}

        {mfaStep ? (
          <form onSubmit={handleMfaSubmit}>
            <div style={{ background: 'rgba(79, 124, 255, 0.15)', border: '1px solid rgba(255,255,255,0.3)', color: '#fff', padding: '10px 12px', borderRadius: 10, fontSize: '0.85rem', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
              <ShieldCheck size={16} />
              <span>{mfaHint || 'Enter the 6-digit code we just sent.'}</span>
            </div>
            <div className="sb-field">
              <label className="sb-label">Verification code</label>
              <input
                className="sb-input"
                placeholder="123456"
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value)}
                inputMode="numeric"
                maxLength={6}
                required
                autoFocus
              />
            </div>
            <motion.button
              type="submit"
              className="sb-btn sb-btn-block"
              style={{ background: 'rgba(255,255,255,0.95)', color: '#4f7cff', fontWeight: 700, marginTop: 8 }}
              disabled={loading}
              whileTap={{ scale: 0.98 }}
            >
              {loading ? <Loader2 size={18} style={{ animation: 'sb-spin 0.9s linear infinite' }} /> : <ShieldCheck size={18} />}
              {loading ? 'Verifying…' : 'Verify & continue'}
            </motion.button>
            <button type="button" onClick={() => setMfaStep(false)} style={{ display: 'block', margin: '10px auto 0', background: 'none', border: 'none', color: 'rgba(255,255,255,0.85)', cursor: 'pointer', textDecoration: 'underline' }}>
              Back to password
            </button>
          </form>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="sb-field">
              <label className="sb-label">Username</label>
              <input
                className="sb-input"
                placeholder="Enter your username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="sb-field">
              <label className="sb-label">Password</label>
              <input
                className="sb-input"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <motion.button
              type="submit"
              className="sb-btn sb-btn-block"
              style={{ background: 'rgba(255,255,255,0.95)', color: '#4f7cff', fontWeight: 700, marginTop: 8 }}
              disabled={loading}
              whileTap={{ scale: 0.98 }}
            >
              {loading ? <Loader2 size={18} style={{ animation: 'sb-spin 0.9s linear infinite' }} /> : <LogIn size={18} />}
              {loading ? 'Signing in…' : 'Sign in'}
            </motion.button>
          </form>
        )}

        <div style={{ textAlign: 'center', marginTop: 18, color: 'rgba(255,255,255,0.9)', fontSize: '0.88rem' }}>
          Don't have an account?{' '}
          <Link to="/register" className="sb-auth-link">Create one</Link>
        </div>

        <div style={{ marginTop: 24, padding: 14, borderRadius: 12, background: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.2)', fontSize: '0.8rem' }}>
          <div style={{ fontWeight: 700, marginBottom: 4, opacity: 0.9 }}>Demo credentials</div>
          <div style={{ opacity: 0.85, lineHeight: 1.6 }}>
            Admin: <b>admin</b> / admin123<br />
            Customer: <b>john</b> / john123
          </div>
        </div>
      </motion.div>
    </div>
  );
}
