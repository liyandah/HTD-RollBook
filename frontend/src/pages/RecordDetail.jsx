import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import http, { API_BASE_URL } from '../api/apiClient';
import { AlertCircle, CheckCircle2, MessageCircle, User, XCircle } from 'lucide-react';
import {
  getBrigadeEligibilityLabel,
  getBrigadeEligibilityTagClass,
  getDepartmentTagClass,
} from '../utils/departmentTags';
import {
  getMissingRequiredRegistrationFields,
  isMissingRequiredField,
  sanitizeRequiredField,
} from '../utils/registrationFields';
import { getCorpsDisplayName } from '../utils/corpsName';
import Badge from '../components/common/Badge';
import Modal from '../components/common/Modal';
import Toast from '../components/common/Toast';
import LoadingSkeleton from '../components/common/LoadingSkeleton';
import EditRecordModal from '../components/modals/EditRecordModal';
import DeleteConfirmationModal from '../components/modals/DeleteConfirmationModal';

const RecordDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(null);
  const [toast, setToast] = useState(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isDeclineModalOpen, setIsDeclineModalOpen] = useState(false);
  const [declineReason, setDeclineReason] = useState('');
  const [isKinModalOpen, setIsKinModalOpen] = useState(false);
  const [kinNameInput, setKinNameInput] = useState('');
  const [kinPhoneInput, setKinPhoneInput] = useState('');
  const [connections, setConnections] = useState({ family: [], friends: [] });
  const [inlineProfileForm, setInlineProfileForm] = useState({
    phoneNumber: '',
    homeAddress: '',
    nextOfKinName: '',
    nextOfKinPhone: '',
  });
  const [isInlineSaving, setIsInlineSaving] = useState(false);

  const resolveImageSrc = (urlPath) => {
    if (!urlPath) return null;
    if (urlPath.startsWith('http://') || urlPath.startsWith('https://')) return urlPath;
    // Static uploads are served at site root (/uploads), not under /api
    if (urlPath.startsWith('/uploads') || urlPath.startsWith('/api/images')) return urlPath;
    return `${API_BASE_URL}${urlPath.startsWith('/') ? '' : '/'}${urlPath}`;
  };

  const isMissing = (value) => isMissingRequiredField(value);

  const renderRequiredFieldValue = (value) => {
    if (isMissingRequiredField(value)) {
      return <span className="text-red-600 font-medium">Missing — please complete</span>;
    }
    return <span className="text-gray-900">{String(value).trim()}</span>;
  };

  const getWhatsAppTarget = () => {
    const raw = (record?.phoneNumber || record?.waId || '').toString();
    const digits = raw.replace(/[^\d]/g, '');
    return digits.length >= 9 ? digits : null;
  };

  const sendReminder = (field) => {
    const target = getWhatsAppTarget();
    if (!target) {
      setToast({ message: 'No valid WhatsApp/contact number found for this member.', type: 'error' });
      return;
    }
    const firstName = record?.firstName || 'Member';
    let message = `Hi ${firstName}, just a quick reminder from HT-E Corps. `;
    if (field === 'kin') {
      message += 'We are missing your Next of Kin details for the Roll Book. Please update them at your earliest convenience.';
    } else if (field === 'id') {
      message += 'We need your National ID number to verify your record. Thank you.';
    } else if (field === 'contact') {
      message += 'Please update your contact number so the Corps can reach you when needed.';
    } else if (field === 'photo') {
      message += 'Please upload a clear profile photo to finish your registration.';
    } else if (field === 'song') {
      message += 'Please share your favourite song to complete your Roll Book profile.';
    } else if (field === 'verse') {
      message += 'Please share your favourite Bible verse to complete your Roll Book profile.';
    }
    const url = `https://wa.me/${target}?text=${encodeURIComponent(message)}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  useEffect(() => {
    fetchRecord();
  }, [id]);

  useEffect(() => {
    if (isKinModalOpen) {
      setKinNameInput(record?.nextOfKinName || '');
      setKinPhoneInput(record?.nextOfKinPhone || '');
    }
  }, [isKinModalOpen, record]);

  useEffect(() => {
    if (!record) return;
    setInlineProfileForm({
      phoneNumber: record.phoneNumber || '',
      homeAddress: record.homeAddress || record.address || '',
      nextOfKinName: record.nextOfKinName || '',
      nextOfKinPhone: record.nextOfKinPhone || '',
    });
  }, [record]);

  const fetchRecord = async () => {
    try {
      setLoading(true);
      const response = await http.get(`/api/records/${id}`);
      if (response.data) {
        setRecord(response.data);
        try {
          const connectionsResponse = await http.get(`/api/records/${id}/connections`);
          setConnections(connectionsResponse.data || { family: [], friends: [] });
        } catch (connectionsErr) {
          console.warn('Failed to load connections, continuing with record view:', connectionsErr);
          setConnections({ family: [], friends: [] });
        }
      } else {
        setToast({ message: 'Record not found', type: 'error' });
        setRecord(null);
      }
    } catch (err) {
      console.error('Failed to load record:', err);
      const errorMessage = err.response?.data?.message || err.message || 'Failed to load record';
      setToast({ message: errorMessage, type: 'error' });
      setRecord(null);
    } finally {
      setLoading(false);
    }
  };

  const handleStatusUpdate = async (newStatus, reason = '') => {
    try {
      await http.patch(`/api/records/${id}/status`, {
        status: newStatus,
        declineReason: reason || undefined,
      });
      setRecord((prev) => ({ ...prev, status: newStatus }));
      const message = newStatus === 'DECLINED' && reason
        ? `Record declined. Reason captured: ${reason}`
        : 'Status updated successfully';
      setToast({ message, type: 'success' });
    } catch (err) {
      console.error('Failed to update status:', err);
      setToast({ message: 'Failed to update status', type: 'error' });
    }
  };

  const submitDecline = async () => {
    if (!declineReason.trim()) {
      setToast({ message: 'Please provide a reason before declining.', type: 'error' });
      return;
    }
    await handleStatusUpdate('DECLINED', declineReason.trim());
    setIsDeclineModalOpen(false);
    setDeclineReason('');
  };

  const handleUpdateRecord = async (recordId, updatedData) => {
    try {
      const response = await http.put(`/api/records/${recordId}`, updatedData);
      setRecord(response.data);
      setToast({ message: 'Record updated successfully!', type: 'success' });
      setIsEditModalOpen(false);
    } catch (err) {
      console.error('Failed to update record:', err);
      const errorMessage = err.response?.data?.message || 'Failed to update record';
      setToast({ message: errorMessage, type: 'error' });
      throw new Error(errorMessage);
    }
  };

  const saveNextOfKinManually = async () => {
    const nextOfKinName = sanitizeRequiredField(kinNameInput);
    const nextOfKinPhone = sanitizeRequiredField(kinPhoneInput);
    if (!nextOfKinName || !nextOfKinPhone) {
      setToast({ message: 'Please provide valid next of kin name and phone (not N/A or placeholder text).', type: 'error' });
      return;
    }
    try {
      const response = await http.put(`/api/records/${id}`, {
        nextOfKinName,
        nextOfKinPhone,
      });
      setRecord(response.data);
      setIsKinModalOpen(false);
      setToast({ message: 'Next of Kin details added successfully.', type: 'success' });
    } catch (err) {
      console.error('Failed to update Next of Kin:', err);
      setToast({ message: 'Failed to save Next of Kin details.', type: 'error' });
    }
  };

  const handleDeleteRecord = async () => {
    try {
      await http.delete(`/api/records/${id}`);
      setToast({ message: 'Record deleted successfully!', type: 'success' });
      navigate('/records');
    } catch (err) {
      console.error('Failed to delete record:', err);
      const errorMessage = err.response?.data?.message || 'Failed to delete record';
      setToast({ message: errorMessage, type: 'error' });
    }
  };

  const handleInlineProfileChange = (field, value) => {
    setInlineProfileForm((prev) => ({ ...prev, [field]: value }));
  };

  const saveInlineProfile = async (event) => {
    event.preventDefault();
    const phoneNumber = sanitizeRequiredField(inlineProfileForm.phoneNumber);
    const nextOfKinName = sanitizeRequiredField(inlineProfileForm.nextOfKinName);
    const nextOfKinPhone = sanitizeRequiredField(inlineProfileForm.nextOfKinPhone);
    const homeAddress = inlineProfileForm.homeAddress.trim();

    if (inlineProfileForm.phoneNumber.trim() && !phoneNumber) {
      setToast({ message: 'Please enter a valid mobile number (not N/A or placeholder text).', type: 'error' });
      return;
    }
    if (inlineProfileForm.nextOfKinName.trim() && !nextOfKinName) {
      setToast({ message: 'Please enter a valid next of kin name (not N/A or placeholder text).', type: 'error' });
      return;
    }
    if (inlineProfileForm.nextOfKinPhone.trim() && !nextOfKinPhone) {
      setToast({ message: 'Please enter a valid next of kin mobile number (not N/A or placeholder text).', type: 'error' });
      return;
    }

    try {
      setIsInlineSaving(true);
      const payload = {
        phoneNumber,
        homeAddress,
        nextOfKinName,
        nextOfKinPhone,
      };
      if (record?.idNumber?.trim()) {
        payload.idNumber = record.idNumber.trim();
      }
      const response = await http.put(`/api/records/${id}`, payload);
      setRecord(response.data);
      setToast({ message: 'Personal details updated successfully.', type: 'success' });
    } catch (err) {
      console.error('Failed to save inline personal details:', err);
      const errorMessage = err.response?.data?.message || 'Failed to save personal details.';
      setToast({ message: errorMessage, type: 'error' });
    } finally {
      setIsInlineSaving(false);
    }
  };

  const requestCertificateUpload = async () => {
    try {
      let response;
      try {
        response = await http.patch(`/api/members/${id}/request-upload`);
      } catch (memberApiError) {
        response = await http.post(`/api/records/${id}/request-certificate-upload`);
      }
      await fetchRecord();
      setToast({
        message: response.data?.message || 'Certificate upload request sent.',
        type: 'success',
      });
    } catch (err) {
      console.error('Failed to request certificate upload:', err);
      setToast({ message: 'Failed to request certificate upload.', type: 'error' });
    }
  };

  if (loading) {
    return (
      <div>
        <button
          onClick={() => navigate('/records')}
          className="mb-6 text-sa-blue hover:text-blue-800 font-medium"
        >
          ← Back to Records
        </button>
        <LoadingSkeleton count={5} />
      </div>
    );
  }

  if (!record && !loading) {
    return (
      <div className="space-y-6">
        {toast && (
          <Toast
            message={toast.message}
            type={toast.type}
            onClose={() => setToast(null)}
          />
        )}
        <button
          onClick={() => navigate('/records')}
          className="mb-6 text-sa-blue hover:text-blue-800 font-medium"
        >
          ← Back to Records
        </button>
        <div className="bg-white rounded-lg shadow-sm p-8 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 mb-4">
            <span className="text-red-600 text-2xl">⚠</span>
          </div>
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Record Not Found</h2>
          <p className="text-gray-600 mb-6">
            The record you're looking for doesn't exist or may have been deleted.
          </p>
          <button
            onClick={() => navigate('/records')}
            className="px-4 py-2 bg-brand-red text-white rounded-lg hover:bg-brand-redDark transition-colors"
          >
            Go Back to Records
          </button>
        </div>
      </div>
    );
  }

  const brigadeEligibilityLabel = getBrigadeEligibilityLabel(record);
  const personPhotoSrc = resolveImageSrc(record.personImageUrl);
  const certPhotoSrc = resolveImageSrc(record.certImageUrl);
  const completionChecks = [
    { label: 'Name', missing: isMissing(record.firstName) || isMissing(record.familyName) },
    { label: 'Mobile Number', missing: isMissing(record.phoneNumber) },
    ...(record.age != null && record.age >= 18
      ? [{ label: 'ID Number', missing: isMissing(record.idNumber) }]
      : []),
    { label: 'Photo', missing: isMissing(record.personImageUrl) },
    { label: 'Next of Kin Name', missing: isMissing(record.nextOfKinName) },
    { label: 'Next of Kin Mobile', missing: isMissing(record.nextOfKinPhone) },
    { label: 'Favourite Song', missing: isMissing(record.favoriteSong) },
    { label: 'Favourite Bible Verse', missing: isMissing(record.favoriteBibleVerse) },
  ];
  const missingRequiredRegistrationFields = getMissingRequiredRegistrationFields(record);
  const hasIncompleteRequiredProfile = missingRequiredRegistrationFields.length > 0;
  const missingItemLabels = completionChecks.filter((item) => item.missing).map((item) => item.label);
  const missingProfileItems = missingItemLabels.length;
  const profileCompletion = Math.max(0, Math.round(((completionChecks.length - missingProfileItems) / completionChecks.length) * 100));
  const progressColorClass = profileCompletion === 100 ? 'bg-green-500' : 'bg-orange-500';
  const hasInlineProfileChanges =
    (inlineProfileForm.phoneNumber || '').trim() !== (record?.phoneNumber || '').trim()
    || (inlineProfileForm.homeAddress || '').trim() !== (record?.homeAddress || record?.address || '').trim()
    || (inlineProfileForm.nextOfKinName || '').trim() !== (record?.nextOfKinName || '').trim()
    || (inlineProfileForm.nextOfKinPhone || '').trim() !== (record?.nextOfKinPhone || '').trim();

  return (
    <div className="w-full min-w-0">
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      <button
        onClick={() => navigate('/records')}
        className="mb-6 text-sa-blue hover:text-blue-800 font-medium"
      >
        ← Back to Records
      </button>

      <div className="space-y-6">
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col md:flex-row items-center md:items-start gap-6">
          <div className="flex-shrink-0 relative">
            <div className="w-[100px] h-[100px] rounded-full border-4 border-white shadow-lg overflow-hidden bg-[#C21124] flex items-center justify-center">
              {personPhotoSrc ? (
                <img
                  src={personPhotoSrc}
                  alt={`${record.firstName || ''} ${record.familyName || ''}`.trim() || 'Profile'}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                    const fallback = e.currentTarget.nextElementSibling;
                    if (fallback) fallback.classList.remove('hidden');
                  }}
                />
              ) : null}
              <div className={`${personPhotoSrc ? 'hidden' : ''} w-full h-full flex items-center justify-center`}>
                <User className="w-10 h-10 text-white" />
              </div>
            </div>
            <div className="absolute bottom-1 right-1 w-5 h-5 bg-green-500 border-2 border-white rounded-full" />
          </div>

          <div className="flex-1 min-w-0 text-center md:text-left">
            <div className="flex flex-wrap items-center gap-2 justify-center md:justify-start">
              <h1 className="text-3xl font-bold text-gray-900">
                {record.firstName} {record.familyName}
              </h1>
              <Badge status={record.status} />
              {record.department && (
                <span className={`dept-tag ${getDepartmentTagClass(record.department)}`}>{record.department}</span>
              )}
              {(record.kidsCount || 0) > 0 && (
                <span className="px-3 py-1 bg-orange-50 text-orange-700 text-xs font-semibold rounded-full border border-orange-100">
                  Children: {record.kidsCount}
                </span>
              )}
            </div>
            <p className="text-sm text-gray-500 mt-2">
              Record ID: <span className="font-mono text-gray-700">{record.recordCode || record.id}</span>
            </p>
            <p className="text-sm text-gray-500">
              Primary Contact:{' '}
              {isMissing(record.phoneNumber) ? (
                <span className="font-medium text-red-600">Missing — please complete</span>
              ) : (
                <span className="font-medium text-gray-800">{record.phoneNumber}</span>
              )}
              {isMissing(record.phoneNumber) && (
                <button
                  onClick={() => sendReminder('contact')}
                  className="ml-2 inline-flex items-center justify-center w-6 h-6 rounded-full bg-green-500 text-white hover:bg-green-600"
                  title="Send WhatsApp reminder"
                >
                  <MessageCircle size={13} />
                </button>
              )}
            </p>
            <div className="mt-4">
              {record.status === 'VERIFIED' ? (
                <span className="inline-flex items-center gap-2 rounded-full bg-green-50 border border-green-200 text-green-700 px-4 py-2 text-sm font-semibold">
                  <CheckCircle2 size={16} />
                  Status: Approved
                </span>
              ) : record.status === 'DECLINED' ? (
                <span className="inline-flex items-center gap-2 rounded-full bg-red-50 border border-red-200 text-red-700 px-4 py-2 text-sm font-semibold">
                  <XCircle size={16} />
                  Status: Declined
                </span>
              ) : (
                <div className="flex flex-wrap items-center gap-3 justify-center md:justify-start">
                  <button
                    onClick={() => handleStatusUpdate('VERIFIED')}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#C21124] text-white font-semibold hover:bg-[#a40f1f] transition-colors"
                  >
                    <CheckCircle2 size={18} />
                    APPROVE
                  </button>
                  <button
                    onClick={() => setIsDeclineModalOpen(true)}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full border border-[#C21124] text-[#C21124] font-semibold hover:bg-[#fff1f3] transition-colors"
                  >
                    <XCircle size={18} />
                    DECLINE
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => setIsEditModalOpen(true)}
              className="px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-medium"
            >
              Edit
            </button>
            <button
              onClick={() => setIsDeleteModalOpen(true)}
              className="px-4 py-2 bg-red-600 text-white rounded-xl hover:bg-red-700 transition-colors font-medium"
            >
              Delete
            </button>
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
          <p className="text-sm font-semibold text-gray-800">
            Profile {profileCompletion}% Complete - {missingProfileItems} item(s) missing
          </p>
          <div className="mt-2 h-2 w-full bg-gray-100 rounded-full overflow-hidden">
            <div className={`h-full transition-all duration-500 ${progressColorClass}`} style={{ width: `${profileCompletion}%` }} />
          </div>
          {missingItemLabels.length > 0 && (
            <p className="mt-2 text-sm text-red-600">
              Missing: {missingItemLabels.join(', ')}
            </p>
          )}
        </div>

        {hasIncompleteRequiredProfile && (
          <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 flex gap-3">
            <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-amber-900">Incomplete profile — required registration details missing</p>
              <p className="mt-1 text-sm text-amber-800">
                This member cannot be considered fully registered until the following are completed:
                {' '}{missingRequiredRegistrationFields.join(', ')}.
              </p>
              <p className="mt-2 text-sm text-amber-800">
                Use the update form below to add the missing information.
              </p>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Personal Information</h2>
            <div className="space-y-3">
              <div>
                <label className="text-sm font-medium text-gray-600">Date of Birth</label>
                <p className="text-gray-900">
                  {record.dob ? new Date(record.dob).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                  }) : 'N/A'}
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Age</label>
                <p className="text-gray-900">{record.age ? `${record.age} years` : 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Marital Status</label>
                <p className="text-gray-900">{record.maritalStatus || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Children Count</label>
                <p className="text-gray-900">{record.kidsCount ?? 0}</p>
              </div>
              {record.age != null && record.age >= 18 && (
                <div>
                  <label className="text-sm font-medium text-gray-600">ID Number</label>
                  <p className="text-gray-900">
                    {record.idNumber || 'N/A'}
                    {isMissing(record.idNumber) && (
                      <button
                        onClick={() => sendReminder('id')}
                        className="ml-2 inline-flex items-center justify-center w-6 h-6 rounded-full bg-green-500 text-white hover:bg-green-600"
                        title="Send WhatsApp reminder"
                      >
                        <MessageCircle size={13} />
                      </button>
                    )}
                  </p>
                </div>
              )}
              <div>
                <label className="text-sm font-medium text-gray-600">Contact Number</label>
                <p className="text-gray-900">
                  {renderRequiredFieldValue(record.phoneNumber)}
                  {isMissing(record.phoneNumber) && (
                    <button
                      onClick={() => sendReminder('contact')}
                      className="ml-2 inline-flex items-center justify-center w-6 h-6 rounded-full bg-green-500 text-white hover:bg-green-600"
                      title="Send WhatsApp reminder"
                    >
                      <MessageCircle size={13} />
                    </button>
                  )}
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Home Address</label>
                <p className="text-gray-900">{record.homeAddress || record.address || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">WhatsApp ID</label>
                <p className="text-gray-900">{record.waId || 'N/A'}</p>
              </div>
            </div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4 mt-6">Corps Information</h2>
            <div className="space-y-3">
              <div>
                <label className="text-sm font-medium text-gray-600">Current Corps</label>
                {getCorpsDisplayName(record.corpsName) ? (
                  <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-sm font-medium bg-blue-50 text-blue-700">
                    {getCorpsDisplayName(record.corpsName)}
                  </span>
                ) : (
                  <p className="text-gray-900">N/A</p>
                )}
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Enrolled Corps</label>
                <p className="text-gray-900">{record.enrolledCorpsName || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Ward</label>
                <p className="text-gray-900">{record.ward || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Brigade</label>
                <p className="text-gray-900">{record.brigade || 'N/A'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Fellowship (department)</label>
                <p className="mt-1">
                  <span className={`dept-tag ${getDepartmentTagClass(record.department)}`}>
                    {record.department || 'Not assigned'}
                  </span>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Brigade eligibility (age-based)</label>
                <p className="mt-1">
                  <span className={`dept-tag ${getBrigadeEligibilityTagClass(brigadeEligibilityLabel)}`}>
                    {brigadeEligibilityLabel}
                  </span>
                </p>
              </div>
            </div>

            <h2 className="text-lg font-semibold text-gray-800 mb-4 mt-6">Next of Kin</h2>
            <div className="space-y-3">
              <div>
                <label className="text-sm font-medium text-gray-600">Next of Kin Name</label>
                <p>{renderRequiredFieldValue(record.nextOfKinName)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Next of Kin Phone</label>
                <p>
                  {renderRequiredFieldValue(record.nextOfKinPhone)}
                  {isMissing(record.nextOfKinPhone) && (
                    <button
                      onClick={() => sendReminder('kin')}
                      className="ml-2 inline-flex items-center justify-center w-6 h-6 rounded-full bg-green-500 text-white hover:bg-green-600"
                      title="Send WhatsApp reminder"
                    >
                      <MessageCircle size={13} />
                    </button>
                  )}
                </p>
              </div>
            </div>

            <div className="mt-8 border border-gray-200 rounded-xl p-4 sm:p-5 bg-gray-50">
              <h3 className="text-base font-semibold text-gray-800 mb-3">Update Personal Details</h3>
              <form className="space-y-3" onSubmit={saveInlineProfile}>
                <div>
                  <label className="text-sm font-medium text-gray-700">Primary Contact</label>
                  <input
                    type="text"
                    value={inlineProfileForm.phoneNumber}
                    onChange={(e) => handleInlineProfileChange('phoneNumber', e.target.value)}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                    placeholder="Enter primary contact number"
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">Home Address</label>
                  <textarea
                    value={inlineProfileForm.homeAddress}
                    onChange={(e) => handleInlineProfileChange('homeAddress', e.target.value)}
                    rows={2}
                    className="mt-1 w-full max-w-full box-border rounded-lg border border-gray-300 px-3 py-2 text-sm resize-y"
                    placeholder="Enter home address"
                  />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="text-sm font-medium text-gray-700">Next of Kin Name</label>
                    <input
                      type="text"
                      value={inlineProfileForm.nextOfKinName}
                      onChange={(e) => handleInlineProfileChange('nextOfKinName', e.target.value)}
                      className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                      placeholder="Enter next of kin name"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">Next of Kin Phone</label>
                    <input
                      type="text"
                      value={inlineProfileForm.nextOfKinPhone}
                      onChange={(e) => handleInlineProfileChange('nextOfKinPhone', e.target.value)}
                      className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                      placeholder="Enter next of kin phone"
                    />
                  </div>
                </div>
                <div className="flex justify-end">
                  <p className={`mr-auto text-xs font-medium ${hasInlineProfileChanges ? 'text-amber-600' : 'text-gray-400'}`}>
                    {hasInlineProfileChanges ? 'Unsaved changes' : 'No pending changes'}
                  </p>
                  <button
                    type="submit"
                    disabled={isInlineSaving || !hasInlineProfileChanges}
                    className="px-4 py-2 rounded-lg bg-[#C21124] text-white hover:bg-[#a40f1f] disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {isInlineSaving ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </form>
            </div>

            <h2 className="text-lg font-semibold text-gray-800 mb-4 mt-6">Favorites</h2>
            <div className="space-y-3">
              <div>
                <label className="text-sm font-medium text-gray-600">Favorite Song</label>
                <p>
                  {renderRequiredFieldValue(record.favoriteSong)}
                  {isMissing(record.favoriteSong) && (
                    <button
                      onClick={() => sendReminder('song')}
                      className="ml-2 inline-flex items-center justify-center w-6 h-6 rounded-full bg-green-500 text-white hover:bg-green-600"
                      title="Send WhatsApp reminder"
                    >
                      <MessageCircle size={13} />
                    </button>
                  )}
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-600">Favorite Bible Verse</label>
                <p>
                  {renderRequiredFieldValue(record.favoriteBibleVerse)}
                  {isMissing(record.favoriteBibleVerse) && (
                    <button
                      onClick={() => sendReminder('verse')}
                      className="ml-2 inline-flex items-center justify-center w-6 h-6 rounded-full bg-green-500 text-white hover:bg-green-600"
                      title="Send WhatsApp reminder"
                    >
                      <MessageCircle size={13} />
                    </button>
                  )}
                </p>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Official Certificate</h3>
              {certPhotoSrc ? (
                <>
                  <img
                    src={certPhotoSrc}
                    alt="Certificate"
                    className="w-full rounded-xl border border-gray-200 cursor-zoom-in hover:opacity-90 transition"
                    onClick={() => setSelectedImage(certPhotoSrc)}
                  />
                  <button
                    onClick={() => window.open(certPhotoSrc, '_blank', 'noopener,noreferrer')}
                    className="w-full mt-4 text-sm font-semibold text-blue-600 hover:underline"
                  >
                    View Certificate
                  </button>
                  <button
                    onClick={requestCertificateUpload}
                    className="w-full mt-2 text-xs font-semibold text-[#C21124] border border-[#C21124] rounded-lg py-2 hover:bg-[#fff1f3]"
                  >
                    Request Certificate Re-upload
                  </button>
                </>
              ) : (
                <div className="space-y-3">
                  <div className="text-sm text-gray-500">No Certificate Uploaded</div>
                  <button
                    onClick={requestCertificateUpload}
                    className="w-full text-sm font-semibold text-[#C21124] border border-[#C21124] rounded-lg py-2 hover:bg-[#fff1f3]"
                  >
                    Request Certificate Upload
                  </button>
                </div>
              )}
            </div>

            <div className={`rounded-2xl p-6 ${!isMissing(record.nextOfKinName) && !isMissing(record.nextOfKinPhone) ? 'bg-blue-50 border border-blue-100' : 'bg-blue-50 border-2 border-dashed border-blue-200'}`}>
              <h3 className="text-md font-bold text-blue-800 mb-3 text-center">Next of Kin</h3>
              {!isMissing(record.nextOfKinName) && !isMissing(record.nextOfKinPhone) ? (
                <>
                  <p className="text-sm text-blue-900 font-semibold text-center">{record.nextOfKinName}</p>
                  <p className="text-sm text-blue-700 text-center">{record.nextOfKinPhone}</p>
                </>
              ) : (
                <div className="flex flex-col items-center gap-3">
                  <AlertCircle size={20} className="text-blue-500" />
                  <p className="text-sm text-blue-700 italic text-center">No Next of Kin provided.</p>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setIsKinModalOpen(true)}
                      className="text-xs bg-white border border-blue-200 px-3 py-1 rounded-full hover:bg-blue-100 transition-all shadow-sm"
                    >
                      + Add Manually
                    </button>
                    <button
                      onClick={() => sendReminder('kin')}
                      className="inline-flex items-center gap-1 text-xs bg-green-500 text-white px-3 py-1 rounded-full hover:bg-green-600"
                    >
                      <MessageCircle size={12} />
                      WhatsApp Reminder
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Metadata</h3>
              <div className="space-y-3">
                <div>
                  <label className="text-sm font-medium text-gray-600">Created At</label>
                  <p className="text-gray-900">{new Date(record.createdAt).toLocaleString()}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Last Updated</label>
                  <p className="text-gray-900">{new Date(record.updatedAt).toLocaleString()}</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-50 rounded-2xl border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Connections</h3>
              {connections.family.length === 0 && connections.friends.length === 0 ? (
                <p className="text-sm text-gray-600">This member has not registered anyone else yet.</p>
              ) : (
                <div className="space-y-4">
                  <div>
                    <h4 className="text-sm font-semibold text-gray-700 mb-2">Family</h4>
                    <div className="space-y-2">
                      {connections.family.map((person) => (
                        <div key={person.id} className="bg-white border border-gray-200 rounded-xl px-3 py-2 flex flex-wrap items-center justify-between gap-2">
                          <div>
                            <p className="text-sm font-semibold text-gray-900">{person.firstName} {person.familyName}</p>
                            <p className="text-xs text-gray-500">{person.status || 'PENDING'}</p>
                          </div>
                          <button onClick={() => navigate(`/records/${person.id}`)} className="text-xs text-blue-600 hover:underline">View</button>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold text-gray-700 mb-2">Friends</h4>
                    <div className="space-y-2">
                      {connections.friends.map((person) => (
                        <div key={person.id} className="bg-white border border-gray-200 rounded-xl px-3 py-2 flex flex-wrap items-center justify-between gap-2">
                          <div>
                            <p className="text-sm font-semibold text-gray-900">{person.firstName} {person.familyName}</p>
                            <p className="text-xs text-gray-500">{person.status || 'PENDING'}</p>
                          </div>
                          <button onClick={() => navigate(`/records/${person.id}`)} className="text-xs text-blue-600 hover:underline">View</button>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

      </div>

      {selectedImage && (
        <Modal
          onClose={() => setSelectedImage(null)}
          title="Image Preview"
          maxWidth="max-w-4xl"
        >
          <div className="flex items-center justify-center bg-slate-50 rounded-lg">
            <img 
              src={selectedImage} 
              alt="Preview" 
              className="w-full h-auto max-h-[70vh] object-contain rounded-lg" 
            />
          </div>
        </Modal>
      )}

      {isEditModalOpen && (
        <EditRecordModal
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          onSave={handleUpdateRecord}
          recordData={record}
        />
      )}

      {isDeleteModalOpen && (
        <DeleteConfirmationModal
          isOpen={isDeleteModalOpen}
          onClose={() => setIsDeleteModalOpen(false)}
          onConfirm={handleDeleteRecord}
          itemName={`record ${record.recordCode}`}
        />
      )}

      {isDeclineModalOpen && (
        <Modal onClose={() => setIsDeclineModalOpen(false)} title="Decline Registration">
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Please provide a reason for declining this registration.
            </p>
            <textarea
              value={declineReason}
              onChange={(e) => setDeclineReason(e.target.value)}
              rows={4}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-300"
              placeholder="Type decline reason..."
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setIsDeclineModalOpen(false)}
                className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={submitDecline}
                className="px-4 py-2 rounded-lg bg-[#C21124] text-white hover:bg-[#a40f1f]"
              >
                Confirm Decline
              </button>
            </div>
          </div>
        </Modal>
      )}

      {isKinModalOpen && (
        <Modal onClose={() => setIsKinModalOpen(false)} title="Add Next of Kin">
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-gray-700">Next of Kin Name</label>
              <input
                type="text"
                value={kinNameInput}
                onChange={(e) => setKinNameInput(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                placeholder="Enter name"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">Next of Kin Phone</label>
              <input
                type="text"
                value={kinPhoneInput}
                onChange={(e) => setKinPhoneInput(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                placeholder="Enter phone number"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setIsKinModalOpen(false)}
                className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={saveNextOfKinManually}
                className="px-4 py-2 rounded-lg bg-[#C21124] text-white hover:bg-[#a40f1f]"
              >
                Save
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default RecordDetail;
