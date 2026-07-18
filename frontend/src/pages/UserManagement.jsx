import React, { useState, useEffect } from 'react';
import { 
  Users, UserPlus, Shield, Mail, Clock, 
  MoreVertical, Trash2, Edit, Key, CheckCircle, XCircle 
} from 'lucide-react';
import http from '../api/apiClient';
import Toast from '../components/common/Toast';
import LoadingSkeleton from '../components/common/LoadingSkeleton';
import Modal from '../components/common/Modal';

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [userToDelete, setUserToDelete] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [newUser, setNewUser] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    role: 'VIEWER'
  });

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await http.get('/api/users');
      console.log('Users API response:', {
        status: response.status,
        data: response.data,
        dataType: Array.isArray(response.data) ? 'array' : typeof response.data,
        dataLength: Array.isArray(response.data) ? response.data.length : 'N/A'
      });
      
      // Ensure users is always an array
      const usersData = Array.isArray(response.data) ? response.data : [];
      console.log('Processed users data:', {
        usersCount: usersData.length,
        firstUser: usersData[0]
      });
      
      setUsers(usersData);
    } catch (err) {
      console.error('Failed to load users:', err);
      const errorMessage = err.response?.data?.message || err.message || 'Failed to load users';
      setToast({ message: errorMessage, type: 'error' });
      setUsers([]); // Set to empty array on error
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await http.post('/api/users', newUser);
      setToast({ message: 'User created successfully!', type: 'success' });
      setShowAddModal(false);
      setNewUser({ username: '', email: '', password: '', fullName: '', role: 'VIEWER' });
      fetchUsers();
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Failed to create user';
      setToast({ message: errorMessage, type: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    try {
      await http.delete(`/api/users/${userToDelete}`);
      setToast({ message: 'User deleted successfully!', type: 'success' });
      setShowDeleteModal(false);
      setUserToDelete(null);
      fetchUsers();
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Failed to delete user';
      setToast({ message: errorMessage, type: 'error' });
    }
  };

  const getInitials = (fullName, username) => {
    if (fullName) {
      const names = fullName.split(' ');
      return names.map(n => n[0]).join('').toUpperCase().substring(0, 2);
    }
    return username.substring(0, 2).toUpperCase();
  };

  const getRandomColor = (index) => {
    const colors = ['bg-brand-red', 'bg-blue-600', 'bg-purple-600', 'bg-amber-600', 'bg-emerald-600', 'bg-pink-600'];
    return colors[index % colors.length];
  };

  const formatLastLogin = (lastLogin) => {
    if (!lastLogin) return 'Never';
    const date = new Date(lastLogin);
    const now = new Date();
    const diffMs = now - date;
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffHours < 1) return 'Just now';
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays < 7) return `${diffDays} days ago`;
    return date.toLocaleDateString();
  };

  const getRoleBadgeStyle = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-brand-red/10 text-brand-red border-brand-red/20';
      case 'EDITOR':
        return 'bg-blue-50 text-blue-600 border-blue-200';
      case 'VIEWER':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      default:
        return 'bg-slate-100 text-slate-600 border-slate-200';
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-slate-900">User Management</h1>
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

      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">User Management</h1>
          <p className="text-slate-500 mt-1">Manage access and permissions for team members</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="inline-flex items-center justify-center gap-2 bg-brand-red hover:bg-brand-redDark text-white px-6 py-3 rounded-xl font-bold shadow-lg shadow-red-900/20 transition-all hover:scale-105"
        >
          <UserPlus size={20} strokeWidth={2.5} />
          Add New User
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 text-white p-6 rounded-2xl shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-blue-100 text-sm font-semibold">Total Users</p>
              <h3 className="text-4xl font-bold mt-2">{users?.length || 0}</h3>
            </div>
            <Users size={40} className="opacity-70" />
          </div>
        </div>

        <div className="bg-gradient-to-br from-emerald-500 to-emerald-600 text-white p-6 rounded-2xl shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-emerald-100 text-sm font-semibold">Active Users</p>
              <h3 className="text-4xl font-bold mt-2">{users?.filter(u => u?.status === 'ACTIVE').length || 0}</h3>
            </div>
            <CheckCircle size={40} className="opacity-70" />
          </div>
        </div>

        <div className="bg-gradient-to-br from-purple-500 to-purple-600 text-white p-6 rounded-2xl shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-100 text-sm font-semibold">Admins</p>
              <h3 className="text-4xl font-bold mt-2">{users?.filter(u => u?.role === 'ADMIN').length || 0}</h3>
            </div>
            <Shield size={40} className="opacity-70" />
          </div>
        </div>
      </div>

      {/* Users List */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="text-left py-4 px-6 text-xs font-bold text-slate-600 uppercase tracking-wider">User</th>
                <th className="text-left py-4 px-6 text-xs font-bold text-slate-600 uppercase tracking-wider">Role</th>
                <th className="text-left py-4 px-6 text-xs font-bold text-slate-600 uppercase tracking-wider">Status</th>
                <th className="text-left py-4 px-6 text-xs font-bold text-slate-600 uppercase tracking-wider">Last Login</th>
                <th className="text-right py-4 px-6 text-xs font-bold text-slate-600 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {users && users.length > 0 ? (
                users.map((user, index) => (
                <tr key={user.id} className="hover:bg-slate-50 transition-colors">
                  <td className="py-4 px-6">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 ${getRandomColor(index)} text-white rounded-full flex items-center justify-center font-semibold text-sm`}>
                        {getInitials(user.fullName, user.username)}
                      </div>
                      <div>
                        <p className="font-semibold text-slate-900">{user.fullName || user.username}</p>
                        <p className="text-sm text-slate-500">{user.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-4 px-6">
                    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold border ${getRoleBadgeStyle(user.role)}`}>
                      <Shield size={12} />
                      {user.role}
                    </span>
                  </td>
                  <td className="py-4 px-6">
                    {user.status === 'ACTIVE' ? (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-700 rounded-lg text-xs font-semibold border border-emerald-200">
                        <CheckCircle size={12} />
                        Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-slate-100 text-slate-600 rounded-lg text-xs font-semibold border border-slate-200">
                        <XCircle size={12} />
                        Inactive
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-6">
                    <div className="flex items-center gap-2 text-sm text-slate-600">
                      <Clock size={14} />
                      {formatLastLogin(user.lastLogin)}
                    </div>
                  </td>
                  <td className="py-4 px-6">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => { setUserToDelete(user.id); setShowDeleteModal(true); }}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete user"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" className="py-16 text-center">
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-slate-100 mb-4">
                      <Users size={24} className="text-slate-400" />
                    </div>
                    <h3 className="text-base font-semibold text-slate-900 mb-2">No users found</h3>
                    <p className="text-sm text-slate-500 max-w-sm mx-auto">
                      Users will appear here once they are created
                    </p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add User Modal */}
      {showAddModal && (
        <Modal
          title="Add New User"
          onClose={() => {
            setShowAddModal(false);
            setNewUser({ username: '', email: '', password: '', fullName: '', role: 'VIEWER' });
          }}
        >
          <form onSubmit={handleCreateUser} className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Username *</label>
              <input
                type="text"
                value={newUser.username}
                onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
                className="input-field"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Email *</label>
              <input
                type="email"
                value={newUser.email}
                onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                className="input-field"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Password *</label>
              <input
                type="password"
                value={newUser.password}
                onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                className="input-field"
                required
                minLength={6}
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Full Name</label>
              <input
                type="text"
                value={newUser.fullName}
                onChange={(e) => setNewUser({ ...newUser, fullName: e.target.value })}
                className="input-field"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Role *</label>
              <select
                value={newUser.role}
                onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
                className="input-field"
              >
                <option value="VIEWER">Viewer</option>
                <option value="EDITOR">Editor</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
            <div className="flex gap-3 pt-4">
              <button 
                type="submit" 
                disabled={submitting}
                className="btn-primary flex-1 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? 'Creating...' : 'Create User'}
              </button>
              <button
                type="button"
                disabled={submitting}
                onClick={() => {
                  setShowAddModal(false);
                  setNewUser({ username: '', email: '', password: '', fullName: '', role: 'VIEWER' });
                }}
                className="btn-outline flex-1 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Cancel
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <Modal
          title="Delete User"
          onClose={() => {
            setShowDeleteModal(false);
            setUserToDelete(null);
          }}
        >
          <div className="space-y-4">
            <p className="text-slate-600">
              Are you sure you want to delete this user? This action cannot be undone.
            </p>
            <div className="flex gap-3 pt-4">
              <button
                onClick={confirmDelete}
                className="btn-primary bg-red-600 hover:bg-red-700 flex-1"
              >
                Delete
              </button>
              <button
                onClick={() => {
                  setShowDeleteModal(false);
                  setUserToDelete(null);
                }}
                className="btn-outline flex-1"
              >
                Cancel
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default UserManagement;
