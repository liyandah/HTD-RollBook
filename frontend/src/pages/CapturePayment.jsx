import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Loader2 } from 'lucide-react';
import { 
  createPayment, 
  getActiveCategories, 
  getActiveProjects, 
  getActiveEvents,
  searchMembers 
} from '../api/contributionsApi';
import Toast from '../components/common/Toast';
import LoadingSkeleton from '../components/common/LoadingSkeleton';

const CapturePayment = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState(null);
  
  const [formData, setFormData] = useState({
    memberId: '',
    categoryId: '',
    projectId: '',
    eventId: '',
    amount: '',
    currency: 'USD',
    paymentMethod: '',
    referenceNumber: '',
    notes: '',
  });

  const [categories, setCategories] = useState([]);
  const [projects, setProjects] = useState([]);
  const [events, setEvents] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [memberSearch, setMemberSearch] = useState('');
  const [memberResults, setMemberResults] = useState([]);
  const [selectedMember, setSelectedMember] = useState(null);
  const [showMemberResults, setShowMemberResults] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (memberSearch.length >= 2) {
      const timer = setTimeout(() => {
        searchMembers(memberSearch)
          .then(res => {
            setMemberResults(res.data || []);
            setShowMemberResults(true);
          })
          .catch(err => console.error('Member search failed:', err));
      }, 300);
      return () => clearTimeout(timer);
    } else {
      setMemberResults([]);
      setShowMemberResults(false);
    }
  }, [memberSearch]);

  useEffect(() => {
    if (formData.categoryId) {
      const category = categories.find(c => c.id === formData.categoryId);
      setSelectedCategory(category);
      
      // Clear project/event if category type doesn't match
      if (category) {
        if (category.type !== 'PROJECT') {
          setFormData(prev => ({ ...prev, projectId: '' }));
        }
        if (category.type !== 'EVENT') {
          setFormData(prev => ({ ...prev, eventId: '' }));
        }
      }
    }
  }, [formData.categoryId, categories]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [catsRes, projsRes, eventsRes] = await Promise.all([
        getActiveCategories(),
        getActiveProjects(),
        getActiveEvents(),
      ]);
      setCategories(catsRes.data || []);
      setProjects(projsRes.data || []);
      setEvents(eventsRes.data || []);
    } catch (err) {
      console.error('Failed to load data:', err);
      setToast({ message: 'Failed to load form data', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleMemberSelect = (member) => {
    setSelectedMember(member);
    setFormData(prev => ({ ...prev, memberId: member.id }));
    setMemberSearch(member.fullName || member.email);
    setShowMemberResults(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation
    if (!formData.memberId) {
      setToast({ message: 'Please select a member', type: 'error' });
      return;
    }
    if (!formData.categoryId) {
      setToast({ message: 'Please select a category', type: 'error' });
      return;
    }
    if (!formData.amount || parseFloat(formData.amount) <= 0) {
      setToast({ message: 'Please enter a valid amount', type: 'error' });
      return;
    }
    if (!formData.paymentMethod) {
      setToast({ message: 'Please select a payment method', type: 'error' });
      return;
    }
    
    // Validate project/event based on category type
    if (selectedCategory?.type === 'PROJECT' && !formData.projectId) {
      setToast({ message: 'Please select a project', type: 'error' });
      return;
    }
    if (selectedCategory?.type === 'EVENT' && !formData.eventId) {
      setToast({ message: 'Please select an event', type: 'error' });
      return;
    }

    try {
      setSubmitting(true);
      const payload = {
        memberId: formData.memberId,
        categoryId: formData.categoryId,
        amount: parseFloat(formData.amount),
        currency: formData.currency,
        paymentMethod: formData.paymentMethod,
        referenceNumber: formData.referenceNumber || null,
        notes: formData.notes || null,
      };
      
      if (formData.projectId) payload.projectId = formData.projectId;
      if (formData.eventId) payload.eventId = formData.eventId;

      await createPayment(payload);
      setToast({ message: 'Payment recorded successfully!', type: 'success' });
      
      // Reset form
      setFormData({
        memberId: '',
        categoryId: '',
        projectId: '',
        eventId: '',
        amount: '',
        currency: 'USD',
        paymentMethod: '',
        referenceNumber: '',
        notes: '',
      });
      setSelectedMember(null);
      setMemberSearch('');
      setSelectedCategory(null);
      
      // Optionally navigate to payments list
      setTimeout(() => {
        navigate('/contributions/payments');
      }, 1500);
    } catch (err) {
      console.error('Failed to capture payment:', err);
      const errorMsg = err.response?.data?.message || 'Failed to record payment';
      setToast({ message: errorMsg, type: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <LoadingSkeleton count={8} />
      </div>
    );
  }

  const paymentMethods = ['CASH', 'ECOCASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'OTHER'];

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
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Capture Payment</h1>
        <p className="text-gray-600">Record a new contribution payment</p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 space-y-6">
        {/* Member Search */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-2">
            Member <span className="text-red-500">*</span>
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-5 w-5 text-gray-400" />
            </div>
            <input
              type="text"
              value={memberSearch}
              onChange={(e) => {
                setMemberSearch(e.target.value);
                if (!e.target.value) {
                  setSelectedMember(null);
                  setFormData(prev => ({ ...prev, memberId: '' }));
                }
              }}
              placeholder="Search by name or email..."
              className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
            />
            {showMemberResults && memberResults.length > 0 && (
              <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                {memberResults.map((member) => (
                  <button
                    key={member.id}
                    type="button"
                    onClick={() => handleMemberSelect(member)}
                    className="w-full text-left px-4 py-2 hover:bg-gray-100 transition-colors"
                  >
                    <div className="font-medium text-gray-900">{member.fullName || member.email}</div>
                    <div className="text-sm text-gray-500">{member.email}</div>
                  </button>
                ))}
              </div>
            )}
          </div>
          {selectedMember && (
            <p className="mt-1 text-sm text-gray-600">
              Selected: {selectedMember.fullName || selectedMember.email}
            </p>
          )}
        </div>

        {/* Category */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-2">
            Contribution Type <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.categoryId}
            onChange={(e) => setFormData(prev => ({ ...prev, categoryId: e.target.value }))}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
            required
          >
            <option value="">Select category...</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name} ({cat.type})
              </option>
            ))}
          </select>
        </div>

        {/* Project (if PROJECT type) */}
        {selectedCategory?.type === 'PROJECT' && (
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Project <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.projectId}
              onChange={(e) => setFormData(prev => ({ ...prev, projectId: e.target.value }))}
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
              required
            >
              <option value="">Select project...</option>
              {projects.map((proj) => (
                <option key={proj.id} value={proj.id}>
                  {proj.name}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Event (if EVENT type) */}
        {selectedCategory?.type === 'EVENT' && (
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Event <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.eventId}
              onChange={(e) => setFormData(prev => ({ ...prev, eventId: e.target.value }))}
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
              required
            >
              <option value="">Select event...</option>
              {events.map((event) => (
                <option key={event.id} value={event.id}>
                  {event.name}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Amount */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-2">
            Amount <span className="text-red-500">*</span>
          </label>
          <div className="flex gap-2">
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={formData.amount}
              onChange={(e) => setFormData(prev => ({ ...prev, amount: e.target.value }))}
              placeholder="0.00"
              className="flex-1 px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
              required
            />
            <select
              value={formData.currency}
              onChange={(e) => setFormData(prev => ({ ...prev, currency: e.target.value }))}
              className="px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
            >
              <option value="USD">USD</option>
              <option value="ZWL">ZWL</option>
            </select>
          </div>
        </div>

        {/* Payment Method */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-2">
            Payment Method <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.paymentMethod}
            onChange={(e) => setFormData(prev => ({ ...prev, paymentMethod: e.target.value }))}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
            required
          >
            <option value="">Select method...</option>
            {paymentMethods.map((method) => (
              <option key={method} value={method}>
                {method.replace('_', ' ')}
              </option>
            ))}
          </select>
        </div>

        {/* Reference Number */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-2">
            Reference Number
          </label>
          <input
            type="text"
            value={formData.referenceNumber}
            onChange={(e) => setFormData(prev => ({ ...prev, referenceNumber: e.target.value }))}
            placeholder="Optional reference or receipt number"
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
          />
        </div>

        {/* Notes */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-2">
            Notes
          </label>
          <textarea
            value={formData.notes}
            onChange={(e) => setFormData(prev => ({ ...prev, notes: e.target.value }))}
            placeholder="Additional notes (optional)"
            rows={3}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
          />
        </div>

        {/* Submit Button */}
        <div className="flex gap-4 pt-4">
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 px-6 py-3 bg-brand-red text-white rounded-lg font-semibold hover:bg-brand-redDark disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
          >
            {submitting ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                Recording...
              </>
            ) : (
              'Record Payment'
            )}
          </button>
          <button
            type="button"
            onClick={() => navigate('/contributions/payments')}
            className="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg font-semibold hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};

export default CapturePayment;
