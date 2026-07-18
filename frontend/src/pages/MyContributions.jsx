import React, { useState, useEffect } from 'react';
import { Download, Filter } from 'lucide-react';
import { getPaymentsByMember } from '../api/contributionsApi';
import Toast from '../components/common/Toast';
import LoadingSkeleton from '../components/common/LoadingSkeleton';

const MyContributions = () => {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalThisMonth, setTotalThisMonth] = useState(0);
  const [userId, setUserId] = useState(null);

  useEffect(() => {
    const storedUserId = localStorage.getItem('userId');
    setUserId(storedUserId);
  }, []);

  useEffect(() => {
    if (userId) {
      loadPayments();
    }
  }, [page, userId]);

  const loadPayments = async () => {
    try {
      setLoading(true);
      const response = await getPaymentsByMember(userId, page, 20);
      setPayments(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);

      // Calculate total for this month
      const now = new Date();
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
      const thisMonthPayments = (response.data.content || []).filter(p => {
        const paymentDate = new Date(p.recordedAt);
        return paymentDate >= startOfMonth;
      });
      const total = thisMonthPayments.reduce((sum, p) => sum + (parseFloat(p.amount) || 0), 0);
      setTotalThisMonth(total);
    } catch (err) {
      console.error('Failed to load contributions:', err);
      setToast({ message: 'Failed to load your contributions', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount, currency = 'USD') => {
    if (!amount) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  if (loading && payments.length === 0) {
    return (
      <div className="space-y-6">
        <LoadingSkeleton count={5} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      <div>
        <h1 className="text-2xl font-bold text-gray-800 mb-2">My Contributions</h1>
        <p className="text-gray-600">View your payment history and contributions</p>
      </div>

      {/* Summary Card */}
      <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Total This Month</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">
              {formatCurrency(totalThisMonth)}
            </p>
          </div>
          <div className="w-16 h-16 bg-brand-red/10 rounded-lg flex items-center justify-center">
            <span className="text-2xl">💰</span>
          </div>
        </div>
      </div>

      {/* Payments Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="p-6 border-b border-gray-200 flex justify-between items-center">
          <h2 className="text-lg font-semibold text-gray-800">Payment History</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Date</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Category</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Project/Event</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Amount</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Method</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Reference</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {payments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-gray-500">
                    No payments found
                  </td>
                </tr>
              ) : (
                payments.map((payment) => (
                  <tr key={payment.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {formatDate(payment.recordedAt)}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {payment.categoryName || 'N/A'}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {payment.projectName || payment.eventName || '-'}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-gray-900">
                      {formatCurrency(payment.amount, payment.currency)}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {payment.paymentMethod?.replace('_', ' ') || 'N/A'}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {payment.referenceNumber || '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-6 py-4 border-t border-gray-200 flex justify-between items-center">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Previous
            </button>
            <span className="text-sm text-gray-600">
              Page {page + 1} of {totalPages}
            </span>
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default MyContributions;
