import { useState, useEffect } from 'react';
import { Card, Row, Col, Alert, Spinner, Table, Badge, Button, Form } from 'react-bootstrap';
import { getDashboard, getAllCustomers, getAllAccounts, getFlaggedTransactions, freezeUser, unfreezeUser, getDailyReport } from '../api';

function AdminDashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [flaggedTxns, setFlaggedTxns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('overview');
  const [reportDate, setReportDate] = useState(new Date().toISOString().split('T')[0]);
  const [report, setReport] = useState(null);

  useEffect(() => { fetchAll(); }, []);

  const fetchAll = async () => {
    try {
      const [dashRes, custRes, accRes, flagRes] = await Promise.all([
        getDashboard(), getAllCustomers(), getAllAccounts(), getFlaggedTransactions()
      ]);
      // API interceptor unwraps ApiResponse envelope, so res.data is the inner data
      setDashboard(dashRes?.data);
      setCustomers(custRes?.data || []);
      setAccounts(accRes?.data || []);
      setFlaggedTxns(flagRes?.data || []);
    } catch (err) {
      console.error('Admin fetch error:', err);
      setError('Failed to load admin data: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleFreeze = async (userId) => {
    try { await freezeUser(userId); fetchAll(); } catch (err) { setError('Failed to freeze user.'); }
  };

  const handleUnfreeze = async (userId) => {
    try { await unfreezeUser(userId); fetchAll(); } catch (err) { setError('Failed to unfreeze user.'); }
  };

  const fetchReport = async () => {
    try {
      const res = await getDailyReport(reportDate);
      setReport(res?.data);
    } catch (err) {
      console.error('Report fetch error:', err);
      setError('Failed to load report: ' + (err.response?.data?.message || err.message));
    }
  };

  if (loading) return <div className="text-center mt-5"><Spinner animation="border" /></div>;

  return (
    <div>
      <h3 className="mb-4">Admin Dashboard</h3>
      {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

      <div className="mb-4">
        <Button variant={activeTab === 'overview' ? 'primary' : 'outline-primary'} className="me-2" onClick={() => setActiveTab('overview')}>Overview</Button>
        <Button variant={activeTab === 'customers' ? 'primary' : 'outline-primary'} className="me-2" onClick={() => setActiveTab('customers')}>Customers</Button>
        <Button variant={activeTab === 'accounts' ? 'primary' : 'outline-primary'} className="me-2" onClick={() => setActiveTab('accounts')}>Accounts</Button>
        <Button variant={activeTab === 'flagged' ? 'primary' : 'outline-primary'} className="me-2" onClick={() => setActiveTab('flagged')}>Flagged</Button>
        <Button variant={activeTab === 'reports' ? 'primary' : 'outline-primary'} onClick={() => setActiveTab('reports')}>Reports</Button>
      </div>

      {activeTab === 'overview' && dashboard && (
        <Row>
          <Col md={3} className="mb-3">
            <Card className="text-center h-100 border-primary">
              <Card.Body><h5>Total Users</h5><h3 className="text-primary">{dashboard.totalUsers || 0}</h3></Card.Body>
            </Card>
          </Col>
          <Col md={3} className="mb-3">
            <Card className="text-center h-100 border-success">
              <Card.Body><h5>Total Accounts</h5><h3 className="text-success">{dashboard.totalAccounts || 0}</h3></Card.Body>
            </Card>
          </Col>
          <Col md={3} className="mb-3">
            <Card className="text-center h-100 border-info">
              <Card.Body><h5>Pending Loans</h5><h3 className="text-info">{dashboard.pendingLoans || 0}</h3></Card.Body>
            </Card>
          </Col>
          <Col md={3} className="mb-3">
            <Card className="text-center h-100 border-warning">
              <Card.Body><h5>Open Tickets</h5><h3 className="text-warning">{dashboard.openTickets || 0}</h3></Card.Body>
            </Card>
          </Col>
        </Row>
      )}

      {activeTab === 'customers' && (
        <Card>
          <Card.Header className="bg-primary text-white">All Customers</Card.Header>
          <Card.Body>
            <Table striped hover responsive>
              <thead>
                <tr><th>ID</th><th>Name</th><th>Email</th><th>Username</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {customers.map(c => (
                  <tr key={c.id}>
                    <td>{c.id}</td>
                    <td>{c.fullName}</td>
                    <td>{c.email}</td>
                    <td>{c.username}</td>
                    <td><Badge bg={c.enabled ? 'success' : 'danger'}>{c.enabled ? 'Active' : 'Frozen'}</Badge></td>
                    <td>
                      {c.enabled
                        ? <Button variant="warning" size="sm" onClick={() => handleFreeze(c.id)}>Freeze</Button>
                        : <Button variant="success" size="sm" onClick={() => handleUnfreeze(c.id)}>Unfreeze</Button>
                      }
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      )}

      {activeTab === 'accounts' && (
        <Card>
          <Card.Header className="bg-success text-white">All Accounts</Card.Header>
          <Card.Body>
            <Table striped hover responsive>
              <thead>
                <tr><th>ID</th><th>Account #</th><th>Type</th><th>Balance</th><th>Status</th><th>Owner</th></tr>
              </thead>
              <tbody>
                {accounts.map(a => (
                  <tr key={a.id}>
                    <td>{a.id}</td>
                    <td>{a.accountNumber}</td>
                    <td>{a.accountType}</td>
                    <td>${(a.balance || 0).toLocaleString()}</td>
                    <td><Badge bg={a.status === 'ACTIVE' ? 'success' : 'danger'}>{a.status}</Badge></td>
                    <td>{a.user?.fullName || a.user?.username || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      )}

      {activeTab === 'flagged' && (
        <Card>
          <Card.Header className="bg-warning text-white">Flagged Transactions</Card.Header>
          <Card.Body>
            {flaggedTxns.length === 0 ? (
              <p className="text-muted">No flagged transactions.</p>
            ) : (
              <Table striped hover responsive>
                <thead>
                  <tr><th>ID</th><th>Reference</th><th>Type</th><th>Amount</th><th>Account</th><th>Date</th><th>Reason</th></tr>
                </thead>
                <tbody>
                  {flaggedTxns.map(t => (
                    <tr key={t.id}>
                      <td>{t.id}</td>
                      <td>{t.referenceNumber}</td>
                      <td>{t.transactionType}</td>
                      <td className="text-danger">${(t.amount || 0).toLocaleString()}</td>
                      <td>{t.account?.accountNumber}</td>
                      <td>{new Date(t.transactionDate).toLocaleString()}</td>
                      <td><span className="text-danger">{t.flagReason || 'Suspicious activity'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            )}
          </Card.Body>
        </Card>
      )}

      {activeTab === 'reports' && (
        <Card>
          <Card.Header className="bg-info text-white">Daily Transaction Report</Card.Header>
          <Card.Body>
            <div className="d-flex gap-2 mb-3">
              <Form.Control type="date" value={reportDate} onChange={(e) => setReportDate(e.target.value)} />
              <Button variant="primary" onClick={fetchReport}>Generate</Button>
            </div>
            {report && (
              <div>
                <Row className="mb-3">
                  <Col md={4}><strong>Total Transactions:</strong> {report.totalTransactions || 0}</Col>
                  <Col md={4}><strong>Total Deposit:</strong> ${(report.totalDeposits || 0).toLocaleString()}</Col>
                  <Col md={4}><strong>Total Withdrawal:</strong> ${(report.totalWithdrawals || 0).toLocaleString()}</Col>
                </Row>
                {report.transactions?.length > 0 && (
                  <Table striped hover responsive size="sm">
                    <thead>
                      <tr><th>Ref</th><th>Type</th><th>Amount</th><th>Account</th><th>Time</th></tr>
                    </thead>
                    <tbody>
                      {report.transactions.map(t => (
                        <tr key={t.id}>
                          <td>{t.referenceNumber}</td>
                          <td>{t.transactionType}</td>
                          <td>${(t.amount || 0).toLocaleString()}</td>
                          <td>{t.account?.accountNumber}</td>
                          <td>{new Date(t.transactionDate).toLocaleTimeString()}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                )}
              </div>
            )}
          </Card.Body>
        </Card>
      )}
    </div>
  );
}

export default AdminDashboard;