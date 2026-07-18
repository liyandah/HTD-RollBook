import React, { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Loader2 } from 'lucide-react';
import { getEvents, createEvent, updateEvent, deleteEvent } from '../api/contributionsApi';
import Toast from '../components/common/Toast';
import LoadingSkeleton from '../components/common/LoadingSkeleton';
import Modal from '../components/common/Modal';

const EventsManagement = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    eventType: '',
    startDate: '',
    endDate: '',
    location: '',
    status: 'ACTIVE',
  });

  useEffect(() => {
    loadEvents();
  }, []);

  const loadEvents = async () => {
    try {
      setLoading(true);
      const response = await getEvents();
      setEvents(response.data || []);
    } catch (err) {
      console.error('Failed to load events:', err);
      setToast({ message: 'Failed to load events', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (event = null) => {
    if (event) {
      setEditingEvent(event);
      setFormData({
        name: event.name || '',
        description: event.description || '',
        eventType: event.eventType || '',
        startDate: event.startDate || '',
        endDate: event.endDate || '',
        location: event.location || '',
        status: event.status || 'ACTIVE',
      });
    } else {
      setEditingEvent(null);
      setFormData({
        name: '',
        description: '',
        eventType: '',
        startDate: '',
        endDate: '',
        location: '',
        status: 'ACTIVE',
      });
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingEvent(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      setSubmitting(true);
      const payload = { ...formData };

      if (editingEvent) {
        await updateEvent(editingEvent.id, payload);
        setToast({ message: 'Event updated successfully', type: 'success' });
      } else {
        await createEvent(payload);
        setToast({ message: 'Event created successfully', type: 'success' });
      }
      
      handleCloseModal();
      loadEvents();
    } catch (err) {
      console.error('Failed to save event:', err);
      const errorMsg = err.response?.data?.message || 'Failed to save event';
      setToast({ message: errorMsg, type: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this event?')) {
      return;
    }

    try {
      await deleteEvent(id);
      setToast({ message: 'Event deleted successfully', type: 'success' });
      loadEvents();
    } catch (err) {
      console.error('Failed to delete event:', err);
      const errorMsg = err.response?.data?.message || 'Failed to delete event';
      setToast({ message: errorMsg, type: 'error' });
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <LoadingSkeleton count={5} />
      </div>
    );
  }

  const eventTypes = ['EASTER_CAMP', 'YOUTH_CAMP', 'CONGRESS', 'SPECIAL_EVENT', 'OTHER'];

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
          <h1 className="text-2xl font-bold text-gray-800 mb-2">Events Management</h1>
          <p className="text-gray-600">Manage camps, congress, and other events</p>
        </div>
        <button
          onClick={() => handleOpenModal()}
          className="px-4 py-2 bg-brand-red text-white rounded-lg font-semibold hover:bg-brand-redDark transition-colors flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          New Event
        </button>
      </div>

      {/* Events Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {events.map((event) => (
          <div key={event.id} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <div className="flex justify-between items-start mb-4">
              <div className="flex-1">
                <h3 className="text-lg font-semibold text-gray-900 mb-1">{event.name}</h3>
                <p className="text-sm text-gray-600 mb-2">{event.description || 'No description'}</p>
                {event.eventType && (
                  <span className="inline-block px-2 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded">
                    {event.eventType.replace('_', ' ')}
                  </span>
                )}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => handleOpenModal(event)}
                  className="p-2 text-gray-600 hover:text-brand-red transition-colors"
                >
                  <Edit className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDelete(event.id)}
                  className="p-2 text-gray-600 hover:text-red-600 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            <div className="space-y-2 text-sm">
              {event.startDate && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Start:</span>
                  <span className="text-gray-900">{new Date(event.startDate).toLocaleDateString()}</span>
                </div>
              )}
              {event.endDate && (
                <div className="flex justify-between">
                  <span className="text-gray-600">End:</span>
                  <span className="text-gray-900">{new Date(event.endDate).toLocaleDateString()}</span>
                </div>
              )}
              {event.location && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Location:</span>
                  <span className="text-gray-900">{event.location}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-gray-600">Status:</span>
                <span className={`px-2 py-1 rounded text-xs font-semibold ${
                  event.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
                  event.status === 'COMPLETED' ? 'bg-blue-100 text-blue-800' :
                  'bg-gray-100 text-gray-800'
                }`}>
                  {event.status}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {events.length === 0 && (
        <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
          <p className="text-gray-500">No events found. Create your first event!</p>
        </div>
      )}

      {/* Create/Edit Modal */}
      {isModalOpen && (
        <Modal
          onClose={handleCloseModal}
          title={editingEvent ? 'Edit Event' : 'Create Event'}
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Event Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Description
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                rows={3}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Event Type
              </label>
              <select
                value={formData.eventType}
                onChange={(e) => setFormData(prev => ({ ...prev, eventType: e.target.value }))}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
              >
                <option value="">Select type...</option>
                {eventTypes.map((type) => (
                  <option key={type} value={type}>
                    {type.replace('_', ' ')}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Start Date
                </label>
                <input
                  type="date"
                  value={formData.startDate}
                  onChange={(e) => setFormData(prev => ({ ...prev, startDate: e.target.value }))}
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  End Date
                </label>
                <input
                  type="date"
                  value={formData.endDate}
                  onChange={(e) => setFormData(prev => ({ ...prev, endDate: e.target.value }))}
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Location
              </label>
              <input
                type="text"
                value={formData.location}
                onChange={(e) => setFormData(prev => ({ ...prev, location: e.target.value }))}
                placeholder="Event location"
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Status
              </label>
              <select
                value={formData.status}
                onChange={(e) => setFormData(prev => ({ ...prev, status: e.target.value }))}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-brand-red focus:border-transparent"
              >
                <option value="ACTIVE">Active</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <div className="flex gap-4 pt-4">
              <button
                type="submit"
                disabled={submitting}
                className="flex-1 px-6 py-3 bg-brand-red text-white rounded-lg font-semibold hover:bg-brand-redDark disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Saving...
                  </>
                ) : (
                  editingEvent ? 'Update Event' : 'Create Event'
                )}
              </button>
              <button
                type="button"
                onClick={handleCloseModal}
                className="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg font-semibold hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default EventsManagement;
