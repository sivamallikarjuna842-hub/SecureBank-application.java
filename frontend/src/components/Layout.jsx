import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard, Wallet, ArrowLeftRight, Users, Landmark, PiggyBank,
  CreditCard, LifeBuoy, Bot, ShieldCheck, Bell, Search, Sun, Moon,
  LogOut, Settings, ChevronRight, Menu, X
} from 'lucide-react';
import { useThemeMode } from '../theme/ThemeContext';
import { onConnectionEvent, getConnectionState, startHealthMonitor, stopHealthMonitor } from '../api';
import { useEffect } from 'react';

const NAV_ITEMS = [
  { to: '/dashboard',         label: 'Dashboard',    icon: LayoutDashboard },
  { to: '/accounts',          label: 'Accounts',     icon: Wallet },
  { to: '/transactions',      label: 'Transactions', icon: ArrowLeftRight },
  { to: '/beneficiaries',     label: 'Beneficiaries',icon: Users },
  { to: '/loans',             label: 'Loans',        icon: Landmark },
  { to: '/fixed-deposits',    label: 'Fixed Deposits',icon: PiggyBank },
  { to: '/cards',             label: 'Cards',        icon: CreditCard },
  { to: '/support',           label: 'Support',      icon: LifeBuoy },
  { to: '/chatbot',           label: 'AI Assistant', icon: Bot },
];

const ADMIN_ITEM = { to: '/admin', label: 'Admin Console', icon: ShieldCheck };

function Sidebar({ user, onLogout, isOpen, onClose }) {
  const navigate = useNavigate();
  const location = useLocation();
  const items = [...NAV_ITEMS];
  if (user?.roles?.includes('ROLE_ADMIN')) items.push(ADMIN_ITEM);

  const handleNav = (to) => { navigate(to); onClose?.(); };

  return (
    <>
      <AnimatePresence>
        {isOpen && (
          <motion.div
            className="sb-sidebar-backdrop"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={onClose}
          />
        )}
      </AnimatePresence>

      <aside className={`sb-sidebar ${isOpen ? 'sb-sidebar-open' : ''}`}>
        <div className="sb-sidebar-brand">
          <div className="sb-brand-icon">
            <Landmark size={22} />
          </div>
          <div>
            <div className="sb-brand-name">SecureBank</div>
            <div className="sb-brand-sub">Premium Banking</div>
          </div>
          <button className="sb-sidebar-close d-lg-none" onClick={onClose} aria-label="Close menu">
            <X size={20} />
          </button>
        </div>

        <nav className="sb-nav">
          <div className="sb-nav-section">Main Menu</div>
          {items.map((item) => {
            const Icon = item.icon;
            const active = location.pathname === item.to;
            return (
              <button
                key={item.to}
                onClick={() => handleNav(item.to)}
                className={`sb-nav-item ${active ? 'sb-nav-item-active' : ''}`}
              >
                <Icon size={20} className="sb-nav-icon" />
                <span>{item.label}</span>
                {active && <ChevronRight size={16} className="sb-nav-chev" />}
              </button>
            );
          })}
        </nav>

        <div className="sb-sidebar-footer">
          <div className="sb-upgrade-card">
            <div className="sb-upgrade-title">Go Premium</div>
            <div className="sb-upgrade-sub">Unlock wealth advisory & higher FD rates</div>
            <button className="sb-upgrade-btn">Upgrade</button>
          </div>
        </div>
      </aside>
    </>
  );
}

function ConnectionPill() {
  const [s, setS] = useState(getConnectionState());
  useEffect(() => onConnectionEvent(setS), []);
  const label =
    s.status === 'online'        ? 'Online' :
    s.status === 'offline'       ? 'Offline' :
    s.status === 'connecting'    ? 'Connecting' :
    s.status === 'degraded'      ? 'Degraded' :
    s.status === 'auth_required' ? 'Sign in'   : 'Online';
  return (
    <div className={`sb-conn sb-conn-${s.status}`} title={s.message || label}>
      <div className="sb-conn-dot" />
      <span>{label}</span>
      {s.latency != null && (
        <span className="sb-conn-latency">{s.latency}ms</span>
      )}
    </div>
  );
}

function TopBar({ user, onMenuClick, onLogout }) {
  const { mode, toggle } = useThemeMode();
  const [notifOpen, setNotifOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const navigate = useNavigate();

  const notifications = [
    { id: 1, title: 'Salary credited',     sub: '+$4,200.00 from ACME Corp',  time: '2m ago', tone: 'success' },
    { id: 2, title: 'New login from iPad', sub: 'Chennai, IN — 10:14 AM',     time: '1h ago', tone: 'info' },
    { id: 3, title: 'Bill due soon',       sub: 'Electricity — $84.50 in 3 days', time: '5h ago', tone: 'warning' },
  ];

  return (
    <header className="sb-topbar">
      <button className="sb-icon-btn d-lg-none" onClick={onMenuClick} aria-label="Open menu">
        <Menu size={20} />
      </button>

      <div className="sb-search">
        <Search size={18} />
        <input placeholder="Search transactions, accounts, beneficiaries…" />
        <kbd>⌘K</kbd>
      </div>

      <div className="sb-topbar-actions">
        <ConnectionPill />
        <button className="sb-icon-btn" onClick={toggle} aria-label="Toggle theme">
          {mode === 'light' ? <Moon size={20} /> : <Sun size={20} />}
        </button>

        <div className="sb-dropdown-host">
          <button className="sb-icon-btn sb-icon-btn-badge" onClick={() => { setNotifOpen(o => !o); setProfileOpen(false); }} aria-label="Notifications">
            <Bell size={20} />
            <span className="sb-badge-dot" />
          </button>
          <AnimatePresence>
            {notifOpen && (
              <motion.div
                className="sb-dropdown sb-dropdown-right"
                initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}
              >
                <div className="sb-dropdown-head">
                  <span>Notifications</span>
                  <span className="sb-tag">3 new</span>
                </div>
                {notifications.map((n) => (
                  <div key={n.id} className="sb-notif">
                    <div className={`sb-notif-dot sb-tone-${n.tone}`} />
                    <div className="sb-notif-body">
                      <div className="sb-notif-title">{n.title}</div>
                      <div className="sb-notif-sub">{n.sub}</div>
                    </div>
                    <div className="sb-notif-time">{n.time}</div>
                  </div>
                ))}
                <div className="sb-dropdown-foot">View all activity</div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <div className="sb-dropdown-host">
          <button className="sb-profile" onClick={() => { setProfileOpen(o => !o); setNotifOpen(false); }}>
            <div className="sb-avatar">{(user?.fullName || user?.username || 'U').slice(0, 1).toUpperCase()}</div>
            <div className="sb-profile-text">
              <div className="sb-profile-name">{user?.fullName || user?.username || 'User'}</div>
              <div className="sb-profile-role">{user?.role === 'ROLE_ADMIN' ? 'Administrator' : 'Customer'}</div>
            </div>
          </button>
          <AnimatePresence>
            {profileOpen && (
              <motion.div
                className="sb-dropdown sb-dropdown-right"
                initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}
              >
                <div className="sb-dropdown-head">
                  <div>
                    <div className="sb-profile-name">{user?.fullName || user?.username}</div>
                    <div className="sb-profile-role">{user?.email || 'Signed in'}</div>
                  </div>
                </div>
                <button className="sb-dropdown-item"><Settings size={16} /> Settings</button>
                <button className="sb-dropdown-item sb-dropdown-danger" onClick={onLogout}><LogOut size={16} /> Sign out</button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  );
}

export default function Layout({ user, onLogout }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  return (
    <div className="sb-shell">
      <Sidebar user={user} onLogout={onLogout} isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="sb-main">
        <TopBar user={user} onMenuClick={() => setSidebarOpen(true)} onLogout={onLogout} />
        <main className="sb-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
