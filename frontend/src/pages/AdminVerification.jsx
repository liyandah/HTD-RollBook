import React, { useEffect, useState } from 'react';
import http from '../api/apiClient';
import Toast from '../components/common/Toast';

const AdminVerification = () => {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const fetchPending = async () => {
    try {
      setLoading(true);
      const res = await http.get('/api/admin/pending-candidates');
      setRows(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || 'Failed to load pending candidates' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPending();
  }, []);

  const handleVerify = async (id) => {
    try {
      await http.post(`/api/admin/verify/${id}`);
      setRows((prev) => prev.filter((r) => r.id !== id));
      setToast({ type: 'success', message: 'Candidate verified successfully' });
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || 'Failed to verify candidate' });
    }
  };

  return (
    <div className="space-y-6">
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
      <div>
        <h1 className="text-3xl font-bold text-slate-900">Pending Verifications</h1>
        <p className="text-slate-500 mt-1">Review chatbot answers before approving members.</p>
      </div>
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-600">
                <th className="px-4 py-3 text-left">Name</th>
                <th className="px-4 py-3 text-left">Gender</th>
                <th className="px-4 py-3 text-left">Age</th>
                <th className="px-4 py-3 text-left">Married?</th>
                <th className="px-4 py-3 text-left">Children</th>
                <th className="px-4 py-3 text-left">Suggested Section</th>
                <th className="px-4 py-3 text-left">Registered By</th>
                <th className="px-4 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr><td className="px-4 py-8 text-slate-500" colSpan="8">Loading...</td></tr>
              ) : rows.length === 0 ? (
                <tr><td className="px-4 py-8 text-slate-500" colSpan="8">No pending candidates.</td></tr>
              ) : rows.map((p) => (
                <tr key={p.id}>
                  <td className="px-4 py-3">{p.fullName || p.recordCode}</td>
                  <td className="px-4 py-3">{p.gender || 'N/A'}</td>
                  <td className="px-4 py-3">{p.age ?? 'N/A'}</td>
                  <td className="px-4 py-3">{p.married ? 'Yes' : 'No'}</td>
                  <td className="px-4 py-3">{p.childrenCount ?? 0}</td>
                  <td className="px-4 py-3 font-semibold text-blue-700">{p.assignedSection || 'Unassigned'}</td>
                  <td className="px-4 py-3">{p.registeredBy || 'Self'}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleVerify(p.id)}
                      className="bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1.5 rounded-lg text-sm font-semibold"
                    >
                      Approve & Verify
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminVerification;
