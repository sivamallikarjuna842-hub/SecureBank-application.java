import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import {
  Wallet, TrendingUp, ArrowDownToLine, ArrowUpFromLine, Send, Receipt,
  Landmark, CreditCard, Bell, Sparkles, Plus, ArrowRight, Eye, EyeOff
} from 'lucide-react';
import { StatCard, SectionHead, GlassCard } from '../components/Primitives';
import {
  getAccounts, getMyLoans, getMyFDs, getMyCards, getMiniStatement,
  getBeneficiaries
} from '../api';

const fmt = (n) => `$${(Number(n) || 0).toLocaleString(undefined, { maximumFractionDigits: 2 })}`;

const STAGGER = { hidden: {}, show: { transition: { staggerChildren: 0.06 } } };
const ITEM    = { hidden: { opacity: 0, y: 14 }, show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: [0.16, 1, 0.3, 1] } } };

const TXN_ICON = {
  DEPOSIT:        { Icon: ArrowDownToLine, in: true  },
  TRANSFER_IN:    { Icon: ArrowDownToLine, in: true  },
  WITHDRAWAL:     { Icon: ArrowUpFromLine, in: false },
  TRANSFER_OUT:   { Icon: ArrowUpFromLine, in: false },
};

export default function Dashboard({ user }) {
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState([]);
  const [loans, setLoans]     = useState([]);
  const [fds, setFds]         = useState([]);
  const [cards, setCards]     = useState([]);
  const [txns, setTxns]       = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hidden, setHidden]   = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const [a, l, f, c, b] = await Promise.all([
          getAccounts(), getMyLoans(), getMyFDs(), getMyCards(), getBeneficiaries()
        ]);
        setAccounts(a?.data || []); setLoans(l?.data || []); setFds(f?.data || []); setCards(c?.data || []); setBeneficiaries(b?.data || []);
        if (a?.data && a.data.length > 0) {
          const t = await getMiniStatement(a.data[0].accountNumber);
          setTxns(t?.data || []);
        }
      } catch (_) { /* surfaced by individual pages */ }
      finally { setLoading(false); }
    })();
  }, []);

  const totalBalance  = useMemo(() => accounts.reduce((s, x) => s + (x.balance || 0), 0), [accounts]);
  const savingsBal    = useMemo(() => accounts.filter(a => a.accountType === 'SAVINGS').reduce((s, x) => s + (x.balance || 0), 0), [accounts]);
  const currentBal    = useMemo(() => accounts.filter(a => a.accountType === 'CURRENT').reduce((s, x) => s + (x.balance || 0), 0), [accounts]);
  const activeLoans   = loans.filter(l => l.status === 'ACTIVE');
  const totalLoanAmt  = activeLoans.reduce((s, l) => s + (l.amount || 0), 0);
  const totalFds      = fds.reduce((s, f) => s + (f.amount || 0), 0);
  const cardLimit     = cards.reduce((s, c) => s + (c.creditLimit || 0), 0);

  // Build chart data from real transactions, fall back to demo if empty
  const flowData = useMemo(() => {
    const byDay = {};
    (txns || []).forEach((t) => {
      const d = new Date(t.transactionDate);
      const key = `${d.getMonth() + 1}/${d.getDate()}`;
      if (!byDay[key]) byDay[key] = { day: key, in: 0, out: 0 };
      const amt = Number(t.amount) || 0;
      if (TXN_ICON[t.transactionType]?.in) byDay[key].in += amt;
      else byDay[key].out += amt;
    });
    const arr = Object.values(byDay).sort((a, b) => a.day.localeCompare(b.day));
    if (arr.length === 0) {
      // graceful fallback so the chart never looks empty
      return ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'].map((d) => ({ day: d, in: 0, out: 0 }));
    }
    return arr.slice(-14);
  }, [txns]);

  const accountMix = useMemo(() => {
    const map = {};
    accounts.forEach((a) => { map[a.accountType] = (map[a.accountType] || 0) + (a.balance || 0); });
    const data = Object.entries(map).map(([name, value]) => ({ name, value }));
    return data.length > 0 ? data : [{ name: 'No accounts', value: 1 }];
  }, [accounts]);

  const PIE_COLORS = ['#4f7cff', '#8b5cf6', '#06b6d4', '#ec4899', '#10b981'];

  if (loading) {
    return (
      <div className="sb-loader">
        <div className="sb-spinner" />
        <div>Loading your dashboard…</div>
      </div>
    );
  }

  return (
    <motion.div variants={STAGGER} initial="hidden" animate="show" className="sb-stack">
      {/* === Page header === */}
      <motion.div variants={ITEM} className="sb-page-head">
        <div>
          <h1 className="sb-page-title">Welcome back, {user?.fullName || user?.username} 👋</h1>
          <p className="sb-page-sub">Here's what's happening with your money today.</p>
        </div>
        <div className="sb-page-head-actions">
          <button className="sb-btn sb-btn-soft" onClick={() => setHidden(h => !h)}>
            {hidden ? <Eye size={16} /> : <EyeOff size={16} />}
            {hidden ? 'Show' : 'Hide'} balance
          </button>
          <button className="sb-btn sb-btn-primary" onClick={() => navigate('/accounts')}>
            <Plus size={16} /> New account
          </button>
        </div>
      </motion.div>

      {/* === Hero balance === */}
      <motion.div variants={ITEM} className="sb-hero">
        <div className="sb-hero-row">
          <div>
            <div className="sb-hero-eyebrow">Total Balance</div>
            <div className="sb-hero-balance">{hidden ? '••••••' : fmt(totalBalance)}</div>
            <div className="sb-hero-meta">
              <div className="sb-hero-meta-item">
                <span className="sb-hero-meta-label">Savings</span>
                <span className="sb-hero-meta-value">{hidden ? '••••' : fmt(savingsBal)}</span>
              </div>
              <div className="sb-hero-meta-item">
                <span className="sb-hero-meta-label">Current</span>
                <span className="sb-hero-meta-value">{hidden ? '••••' : fmt(currentBal)}</span>
              </div>
              <div className="sb-hero-meta-item">
                <span className="sb-hero-meta-label">FDs</span>
                <span className="sb-hero-meta-value">{hidden ? '••••' : fmt(totalFds)}</span>
              </div>
              <div className="sb-hero-meta-item">
                <span className="sb-hero-meta-label">Active loans</span>
                <span className="sb-hero-meta-value">{activeLoans.length}</span>
              </div>
            </div>
          </div>
          <div className="sb-hero-actions">
            <button className="sb-btn sb-btn-ghost" onClick={() => navigate('/beneficiaries')}>
              <Send size={16} /> Transfer
            </button>
            <button className="sb-btn sb-btn-ghost" onClick={() => navigate('/transactions')}>
              <Receipt size={16} /> Pay bills
            </button>
          </div>
        </div>
      </motion.div>

      {/* === Stat cards === */}
      <motion.div variants={ITEM} className="sb-stats-grid">
        <StatCard title="Total Balance"   value={hidden ? '••••' : fmt(totalBalance)} sub={`${accounts.length} account${accounts.length === 1 ? '' : 's'}`}   icon={Wallet}    gradient="blue"   trend={4.2} delay={0.00} onClick={() => navigate('/accounts')} />
        <StatCard title="Savings"         value={hidden ? '••••' : fmt(savingsBal)}   sub="Across all savings accounts"     icon={TrendingUp} gradient="green"  trend={2.1} delay={0.05} onClick={() => navigate('/accounts')} />
        <StatCard title="Current Account" value={hidden ? '••••' : fmt(currentBal)}   sub="Operating balance"               icon={Landmark}  gradient="purple" trend={-0.8} delay={0.10} onClick={() => navigate('/accounts')} />
        <StatCard title="Credit Limit"    value={hidden ? '••••' : fmt(cardLimit)}    sub={`${cards.length} card${cards.length === 1 ? '' : 's'}`}    icon={CreditCard} gradient="cyan"   trend={1.4} delay={0.15} onClick={() => navigate('/cards')} />
      </motion.div>

      {/* === Quick actions === */}
      <motion.div variants={ITEM}>
        <SectionHead title="Quick Actions" subtitle="One-tap shortcuts to the most-used features" icon={Sparkles} />
        <div className="sb-quick-grid">
          {[
            { label: 'Send Money',   Icon: Send,          tone: 'blue',   to: '/beneficiaries' },
            { label: 'Deposit',       Icon: ArrowDownToLine, tone: 'green',  to: '/transactions' },
            { label: 'Withdraw',      Icon: ArrowUpFromLine, tone: 'amber',  to: '/transactions' },
            { label: 'Pay Bill',      Icon: Receipt,       tone: 'purple', to: '/transactions' },
            { label: 'New Loan',      Icon: Landmark,      tone: 'cyan',   to: '/loans' },
            { label: 'Request Card',  Icon: CreditCard,    tone: 'blue',   to: '/cards' },
          ].map(({ label, Icon, tone, to }, i) => (
            <button key={label} className="sb-quick" onClick={() => navigate(to)} style={{ animationDelay: `${i * 30}ms` }}>
              <div className={`sb-quick-icon ${tone}`}><Icon size={22} /></div>
              <div>{label}</div>
            </button>
          ))}
        </div>
      </motion.div>

      {/* === Charts row === */}
      <motion.div variants={ITEM} className="sb-grid-2">
        <GlassCard className="sb-chart-card">
          <SectionHead
            title="Cash Flow"
            subtitle="Money in vs money out, last 14 transactions"
            icon={TrendingUp}
            action={<button className="sb-link" onClick={() => navigate('/transactions')}>View all <ArrowRight size={14} /></button>}
          />
          <div className="sb-chart-wrap">
            <ResponsiveContainer>
              <AreaChart data={flowData}>
                <defs>
                  <linearGradient id="gIn"  x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%"   stopColor="#10b981" stopOpacity={0.5} />
                    <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="gOut" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%"   stopColor="#ef4444" stopOpacity={0.5} />
                    <stop offset="100%" stopColor="#ef4444" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(127,127,127,0.15)" />
                <XAxis dataKey="day" tick={{ fontSize: 12, fill: 'var(--text-mute)' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 12, fill: 'var(--text-mute)' }} axisLine={false} tickLine={false} />
                <Tooltip content={<ChartTooltip />} />
                <Area type="monotone" dataKey="in"  name="Money In"  stroke="#10b981" strokeWidth={2.5} fill="url(#gIn)"  />
                <Area type="monotone" dataKey="out" name="Money Out" stroke="#ef4444" strokeWidth={2.5} fill="url(#gOut)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </GlassCard>

        <GlassCard className="sb-chart-card">
          <SectionHead title="Account Mix" subtitle="Distribution by account type" icon={Wallet} />
          <div className="sb-chart-wrap">
            <ResponsiveContainer>
              <PieChart>
                <Pie data={accountMix} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={50} outerRadius={85} paddingAngle={3}>
                  {accountMix.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                </Pie>
                <Tooltip content={<ChartTooltip />} />
                <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </GlassCard>
      </motion.div>

      {/* === Recent transactions + Notifications === */}
      <motion.div variants={ITEM} className="sb-grid-2">
        <GlassCard>
          <SectionHead
            title="Recent Transactions"
            subtitle="Your latest account activity"
            icon={Receipt}
            action={<button className="sb-link" onClick={() => navigate('/transactions')}>See all <ArrowRight size={14} /></button>}
          />
          {txns.length === 0 ? (
            <div className="sb-alert sb-alert-info">No transactions yet. Make your first deposit to get started.</div>
          ) : (
            <div className="sb-txn-list">
              {txns.slice(0, 6).map((t) => {
                const meta = TXN_ICON[t.transactionType] || { Icon: ArrowDownToLine, in: true };
                const { Icon } = meta;
                return (
                  <div key={t.id} className="sb-txn">
                    <div className={`sb-txn-icon ${meta.in ? 'in' : 'out'}`}><Icon size={18} /></div>
                    <div className="sb-txn-body">
                      <div className="sb-txn-title">{t.transactionType?.replace('_', ' ')}</div>
                      <div className="sb-txn-sub">
                        {t.account?.accountNumber} • {new Date(t.transactionDate).toLocaleDateString()}
                      </div>
                    </div>
                    <div className={`sb-txn-amount ${meta.in ? 'in' : 'out'}`}>
                      {meta.in ? '+' : '−'}{fmt(t.amount)}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </GlassCard>

        <GlassCard>
          <SectionHead title="Notifications" subtitle="Activity & alerts" icon={Bell} action={<button className="sb-link">Mark all read</button>} />
          <div className="sb-txn-list">
            {[
              { id: 1, title: 'Salary credited',     sub: '+$4,200.00 from ACME Corp',        time: '2m ago', tone: 'success' },
              { id: 2, title: 'New login from iPad', sub: 'Chennai, IN — 10:14 AM',           time: '1h ago', tone: 'info' },
              { id: 3, title: 'Bill due soon',       sub: 'Electricity — $84.50 in 3 days',   time: '5h ago', tone: 'warning' },
              { id: 4, title: 'FD matured',          sub: 'Your 1-yr FD of $10,000 matured',  time: '1d ago', tone: 'success' },
            ].map((n) => (
              <div key={n.id} className="sb-txn">
                <div className={`sb-txn-icon ${n.tone === 'success' ? 'in' : n.tone === 'warning' ? 'out' : 'in'}`}>
                  <Bell size={18} />
                </div>
                <div className="sb-txn-body">
                  <div className="sb-txn-title">{n.title}</div>
                  <div className="sb-txn-sub">{n.sub}</div>
                </div>
                <div className="sb-txn-amount" style={{ color: 'var(--text-mute)' }}>{n.time}</div>
              </div>
            ))}
          </div>
        </GlassCard>
      </motion.div>

      {/* === Loan & Credit card summary === */}
      <motion.div variants={ITEM} className="sb-grid-2">
        <GlassCard>
          <SectionHead title="Loan Summary" subtitle="Outstanding loans & EMI progress" icon={Landmark} action={<button className="sb-link" onClick={() => navigate('/loans')}>Manage <ArrowRight size={14} /></button>} />
          {activeLoans.length === 0 ? (
            <div className="sb-alert sb-alert-info">No active loans. You're debt-free! 🎉</div>
          ) : (
            <div className="sb-stack">
              {activeLoans.slice(0, 3).map((l) => {
                const progress = Math.max(0, Math.min(100, ((l.amount - (l.outstandingAmount || 0)) / l.amount) * 100));
                return (
                  <div key={l.id}>
                    <div className="sb-row-between" style={{ marginBottom: 6 }}>
                      <div>
                        <div style={{ fontWeight: 700 }}>{l.purpose || 'Personal Loan'}</div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-mute)' }}>{l.interestRate}% • {l.tenureMonths} mo</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontWeight: 800 }}>{fmt(l.amount)}</div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-mute)' }}>{Math.round(progress)}% paid</div>
                      </div>
                    </div>
                    <div className="sb-progress"><span style={{ width: `${progress}%` }} /></div>
                  </div>
                );
              })}
            </div>
          )}
        </GlassCard>

        <GlassCard>
          <SectionHead title="Credit Card Summary" subtitle="Your active cards" icon={CreditCard} action={<button className="sb-link" onClick={() => navigate('/cards')}>All cards <ArrowRight size={14} /></button>} />
          {cards.length === 0 ? (
            <div className="sb-alert sb-alert-info">No active cards. Request one to get started.</div>
          ) : (
            <div className="sb-stack">
              {cards.slice(0, 2).map((c, i) => (
                <div key={c.id} className="sb-cc" style={{ background: i % 2 === 0 ? 'var(--g-primary)' : 'var(--g-purple)' }}>
                  <div className="sb-cc-row">
                    <div className="sb-cc-variant">{(c.cardType || 'CARD').toUpperCase()}</div>
                    <div className="sb-cc-chip" />
                  </div>
                  <div className="sb-cc-number">•••• •••• •••• {String(c.cardNumber || '').slice(-4) || '0000'}</div>
                  <div className="sb-cc-row">
                    <div>
                      <div className="sb-cc-meta-label">Card holder</div>
                      <div className="sb-cc-meta-value">{user?.fullName || user?.username || 'CARD HOLDER'}</div>
                    </div>
                    <div>
                      <div className="sb-cc-meta-label">Limit</div>
                      <div className="sb-cc-meta-value">{fmt(c.creditLimit || 0)}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </GlassCard>
      </motion.div>

      {/* === Beneficiaries quick-access === */}
      {beneficiaries.length > 0 && (
        <motion.div variants={ITEM}>
          <SectionHead title="Quick Transfer" subtitle="Send to a saved beneficiary" icon={Send} action={<button className="sb-link" onClick={() => navigate('/beneficiaries')}>Manage <ArrowRight size={14} /></button>} />
          <GlassCard className="sb-glass-pad-sm">
            <div className="sb-row" style={{ overflowX: 'auto' }}>
              {beneficiaries.slice(0, 6).map((b) => (
                <button
                  key={b.id}
                  className="sb-quick"
                  style={{ minWidth: 110 }}
                  onClick={() => navigate('/transactions')}
                >
                  <div className="sb-quick-icon blue"><Send size={18} /></div>
                  <div style={{ fontSize: '0.8rem' }}>{b.name}</div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-mute)' }}>•••• {String(b.accountNumber || '').slice(-4)}</div>
                </button>
              ))}
            </div>
          </GlassCard>
        </motion.div>
      )}
    </motion.div>
  );
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload || !payload.length) return null;
  return (
    <div className="sb-chart-tooltip">
      {label && <div className="sb-chart-tooltip-label">{label}</div>}
      {payload.map((p, i) => (
        <div key={i} style={{ color: p.color || p.fill, fontWeight: 700 }}>
          {p.name}: ${(p.value || 0).toLocaleString()}
        </div>
      ))}
    </div>
  );
}
