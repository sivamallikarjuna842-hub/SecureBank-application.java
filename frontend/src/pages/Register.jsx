import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Landmark, UserPlus, Loader2, CheckCircle } from 'lucide-react';
import { register, showToast } from '../api';

export default function Register() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    setLoading(true);
    try {
      const res = await register(fullName, email, username, password);
      const data = res.data;
      if (data) {
        setSuccess(true);
        showToast('success', 'Account created successfully! Please sign in.');
        setTimeout(() => navigate('/login'), 2000);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
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
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <CheckCircle size={48} style={{ color: '#4ade80', marginBottom: 12 }} />
            <h2>Account created!</h2>
            <p style={{ color: 'rgba(255,255,255,0.85)', marginBottom: 16 }}>
              Redirecting you to sign in…
            </p>
            <Link to="/login" className="sb-auth-link">Sign in now</Link>
          </div>
        </motion.div>
      </div>
    );
  }

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

        <h2 style={{ textAlign: 'center' }}>Create your account</h2>
        <p style={{ textAlign: 'center', color: 'rgba(255,255,255,0.85)', marginBottom: 24, fontSize: '0.9rem' }}>
          Get started with online banking
        </p>

        {error && <div className="sb-auth-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="sb-field">
            <label className="sb-label">Full Name</label>
            <input
              className="sb-input"
              placeholder="John Doe"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              required
              autoFocus
            />
          </div>
          <div className="sb-field">
            <label className="sb-label">Email</label>
            <input
              className="sb-input"
              type="email"
              placeholder="john@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="sb-field">
            <label className="sb-label">Username</label>
            <input
              className="sb-input"
              placeholder="Choose a username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="sb-field">
            <label className="sb-label">Password</label>
            <input
              className="sb-input"
              type="password"
              placeholder="Min. 6 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
            />
          </div>
          <div className="sb-field">
            <label className="sb-label">Confirm Password</label>
            <input
              className="sb-input"
              type="password"
              placeholder="Repeat your password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={6}
            />
          </div>

          <motion.button
            type="submit"
            className="sb-btn sb-btn-block"
            style={{ background: 'rgba(255,255,255,0.95)', color: '#4f7cff', fontWeight: 700, marginTop: 8 }}
            disabled={loading}
            whileTap={{ scale: 0.98 }}
          >
            {loading ? <Loader2 size={18} style={{ animation: 'sb-spin 0.9s linear infinite' }} /> : <UserPlus size={18} />}
            {loading ? 'Creating account…' : 'Create account'}
          </motion.button>
        </form>

        <div style={{ textAlign: 'center', marginTop: 18, color: 'rgba(255,255,255,0.9)', fontSize: '0.88rem' }}>
          Already have an account?{' '}
          <Link to="/login" className="sb-auth-link">Sign in</Link>
        </div>
      </motion.div>
    </div>
  );
}