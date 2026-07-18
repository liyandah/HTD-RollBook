import React, { useState, useEffect } from 'react';
import { Filter, Download, Eye, Loader2 } from 'lucide-react';
import { getPayments, getPaymentsByMember } from '../api/contributionsApi';
import { getCurrentUser } from '../utils/user';
import Toast from '../components/common/Toast';
import LoadingSkeleton from '../components/common/LoadingSkeleton';

const PaymentsList = () => {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [userRole, setUserRole] = useState('VIEWER');
  const [userId, setUserId] = useState(null);
  const [userInfoLoaded, setUserInfoLoaded] = useState(false);
  const [filters, setFilters] = useState({
    memberId: '',
    categoryId: '',
    projectId: '',
    eventId: '',
    paymentMethod: '',
  });

  useEffect(() => {
    loadUserInfo();
  }, []);

  // Load payments when we have enough info: admins can load immediately; VIEWERs need userId
  useEffect(() => {
    if (!userInfoLoaded) return;
    const canLoadAll = userRole !== 'VIEWER';
    const canLoadMine = userRole === 'VIEWER' && userId;
    if (canLoadAll || canLoadMine) {
      loadPayments();
    } else {
      setLoading(false);
    }
  }, [page, userId, userRole, userInfoLoaded]);

  const loadUserInfo = async () => {
    try {
      const user = await getCurrentUser();
      const role = user?.role || 'VIEWER';
      const id = user?.id ?? localStorage.getItem('userId');
      setUserRole(role);
      setUserId(id || null);
    } catch (err) {
      console.error('Failed to load user info:', err);
      setUserRole('VIEWER');
      setUserId(localStorage.getItem('userId') || null);
    } finally {
      setUserInfoLoaded(true);
    }
  };

  const loadPayments = async () => {
    try {
      setLoading(true);
      let response;
      
      // Members see only their own payments
      if (userRole === 'VIEWER' && userId) {
        response = await getPaymentsByMember(userId, page, 20);
      } else {
        // Admin/Secretary see all payments
        response = await getPayments(page, 20);
      }
      
      setPayments(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (err) {
      console.error('Failed to load payments:', err);
      setToast({ message: 'Failed to load payments', type: 'error' });
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

  const exportToCSV = () => {
    const headers = ['Date', 'Member', 'Category', 'Project/Event', 'Amount', 'Method', 'Reference'];
    const rows = payments.map(p => [
      formatDate(p.recordedAt),
      p.memberName || 'N/A',
      p.categoryName || 'N/A',
      p.projectName || p.eventName || 'N/A',
      `${p.currency} ${p.amount}`,
      p.paymentMethod || 'N/A',
      p.referenceNumber || 'N/A',
    ]);

    const csv = [headers, ...rows].map(row => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `payments-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
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

      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">Payments</h1>
          <p className="text-gray-600">
            {userRole === 'VIEWER' ? 'Your payment history' : 'All payment records'}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={exportToCSV}
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition-colors flex items-center gap-2"
          >
            <Download className="w-4 h-4" />
            Export CSV
          </button>
        </div>
      </div>

      {/* Payments Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Date</th>
                {userRole !== 'VIEWER' && (
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Member</th>
                )}
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Category</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Project/Event</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Amount</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Method</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Reference</th>
                {userRole !== 'VIEWER' && (
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Recorded By</th>
                )}
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {payments.length === 0 ? (
                <tr>
                  <td colSpan={userRole !== 'VIEWER' ? 9 : 7} className="px-6 py-8 text-center text-gray-500">
                    No payments found
                  </td>
                </tr>
              ) : (
                payments.map((payment) => (
                  <tr key={payment.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {formatDate(payment.recordedAt)}
                    </td>
                    {userRole !== 'VIEWER' && (
                      <td className="px-6 py-4 text-sm text-gray-900">
                        {payment.memberName || payment.memberEmail || 'N/A'}
                      </td>
                    )}
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
                    {userRole !== 'VIEWER' && (
                      <td className="px-6 py-4 text-sm text-gray-600">
                        {payment.recordedByName || 'N/A'}
                      </td>
                    )}
                    <td className="px-6 py-4 text-sm">
                      <button
                        onClick={() => {
                          // TODO: Open payment details modal
                          setToast({ message: 'Payment details coming soon', type: 'info' });
                        }}
                        className="text-brand-red hover:text-brand-redDark transition-colors"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
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

export default PaymentsList;
