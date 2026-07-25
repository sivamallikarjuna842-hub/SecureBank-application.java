import { useState, useEffect } from 'react';
import { Card, Button, Modal, Form, Alert, Spinner, Row, Col, Table, Badge } from 'react-bootstrap';
import { getAccounts, getTransactions, deposit, withdraw, transfer, transferToBeneficiary, getBeneficiaries } from '../api';

function Transactions() {
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [txnType, setTxnType] = useState('DEPOSIT');
  const [formData, setFormData] = useState({ amount: '', description: '', toAccount: '', beneficiaryId: '' });
  const [submitting, setSubmitting] = useState(false);
  const [txnLoading, setTxnLoading] = useState(false);

  useEffect(() => {
    fetchAccounts();
    fetchBeneficiaries();
  }, []);

  const fetchAccounts = async () => {
    try {
      const res = await getAccounts();
      setAccounts(res.data);
      if (res.data.length > 0) {
        setSelectedAccount(res.data[0].accountNumber);
        fetchTxns(res.data[0].accountNumber);
      }
    } catch (err) {
      setError('Failed to load accounts.');
    } finally {
      setLoading(false);
    }
  };

  const fetchBeneficiaries = async () => {
    try {
      const res = await getBeneficiaries();
      setBeneficiaries(res.data);
    } catch (err) { /* ignore */ }
  };

  const fetchTxns = async (accNo) => {
    setTxnLoading(true);
    try {
      const res = await getTransactions(accNo);
      setTransactions(res.data);
    } catch (err) {
      setError('Failed to load transactions.');
    } finally {
      setTxnLoading(false);
    }
  };

  const handleAccountChange = (accNo) => {
    setSelectedAccount(accNo);
    fetchTxns(accNo);
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      const amount = parseFloat(formData.amount);
      if (txnType === 'DEPOSIT') {
        await deposit(selectedAccount, amount, formData.description);
      } else if (txnType === 'WITHDRAW') {
        await withdraw(selectedAccount, amount, formData.description);
      } else if (txnType === 'TRANSFER') {
        await transfer(selectedAccount, formData.toAccount, amount, formData.description);
      } else if (txnType === 'BENEFICIARY') {
        await transferToBeneficiary(selectedAccount, formData.beneficiaryId, amount, formData.description);
      }
      setShowModal(false);
      setFormData({ amount: '', description: '', toAccount: '', beneficiaryId: '' });
      fetchTxns(selectedAccount);
    } catch (err) {
      setError(err.response?.data?.message || 'Transaction failed.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="text-center mt-5"><Spinner animation="border" /></div>;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h3>Transactions</h3>
        <Button variant="primary" onClick={() => setShowModal(true)} disabled={!selectedAccount}>+ New Transaction</Button>
      </div>
      {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

      <Card className="mb-4">
        <Card.Body>
          <Form.Group>
            <Form.Label>Select Account</Form.Label>
            <Form.Select value={selectedAccount} onChange={(e) => handleAccountChange(e.target.value)}>
              {accounts.map(acc => (
                <option key={acc.id} value={acc.accountNumber}>
                  {acc.accountType} - {acc.accountNumber} (${acc.balance})
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Card.Body>
      </Card>

      <Card>
        <Card.Header className="bg-primary text-white">Transaction History</Card.Header>
        <Card.Body>
          {txnLoading ? (
            <div className="text-center p-3"><Spinner animation="border" size="sm" /></div>
          ) : transactions.length === 0 ? (
            <p className="text-muted text-center">No transactions found.</p>
          ) : (
            <Table striped hover responsive>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Reference</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Description</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map(txn => (
                  <tr key={txn.id}>
                    <td>{new Date(txn.transactionDate).toLocaleString()}</td>
                    <td><small>{txn.transactionReference}</small></td>
                    <td>{txn.transactionType}</td>
                    <td className={['DEPOSIT', 'CREDIT', 'TRANSFER_IN'].includes(txn.transactionType) ? 'text-success fw-bold' : 'text-danger fw-bold'}>
                      {['DEPOSIT', 'CREDIT', 'TRANSFER_IN'].includes(txn.transactionType) ? '+' : '-'}${Math.abs(txn.amount || 0).toLocaleString()}
                    </td>
                    <td>{txn.description || '-'}</td>
                    <td><Badge bg={txn.flagged ? 'warning' : 'success'}>{txn.flagged ? 'FLAGGED' : 'COMPLETED'}</Badge></td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      <Modal show={showModal} onHide={() => setShowModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>New Transaction</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Transaction Type</Form.Label>
              <Form.Select value={txnType} onChange={(e) => setTxnType(e.target.value)}>
                <option value="DEPOSIT">Deposit</option>
                <option value="WITHDRAW">Withdraw</option>
                <option value="TRANSFER">Transfer to Account</option>
                <option value="BENEFICIARY">Transfer to Beneficiary</option>
              </Form.Select>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Amount</Form.Label>
              <Form.Control type="number" step="0.01" min="0.01" placeholder="Enter amount" value={formData.amount} onChange={(e) => setFormData({...formData, amount: e.target.value})} required />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control type="text" placeholder="Optional description" value={formData.description} onChange={(e) => setFormData({...formData, description: e.target.value})} />
            </Form.Group>
            {txnType === 'TRANSFER' && (
              <Form.Group className="mb-3">
                <Form.Label>To Account Number</Form.Label>
                <Form.Control type="text" placeholder="Enter destination account number" value={formData.toAccount} onChange={(e) => setFormData({...formData, toAccount: e.target.value})} required />
              </Form.Group>
            )}
            {txnType === 'BENEFICIARY' && (
              <Form.Group className="mb-3">
                <Form.Label>Beneficiary</Form.Label>
                <Form.Select value={formData.beneficiaryId} onChange={(e) => setFormData({...formData, beneficiaryId: e.target.value})} required>
                  <option value="">Select beneficiary</option>
                  {beneficiaries.filter(b => b.verified).map(b => (
                    <option key={b.id} value={b.id}>{b.name} - {b.accountNumber}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            )}
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
          <Button variant="primary" onClick={handleSubmit} disabled={submitting}>
            {submitting ? 'Processing...' : 'Submit'}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}

export default Transactions;