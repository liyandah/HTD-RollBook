import React, { useState, useEffect, useMemo } from 'react';
import { X, User, Building2, Calendar, ShieldCheck, Save, AlertCircle, Image } from 'lucide-react';
import http from '../../api/apiClient'; // Use shared apiClient instance

const DEFAULT_CORPS_NAME = 'Highfield Temple';

const calculateAgeFromDob = (dob) => {
  if (!dob) return null;
  const birthDate = new Date(dob);
  if (Number.isNaN(birthDate.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age -= 1;
  }
  return age;
};

const EditRecordModal = ({ isOpen, onClose, onSave, recordData = null }) => {
  const isEdit = Boolean(recordData?.id);

  const [formData, setFormData] = useState({
    waId: '',
    firstName: '',
    familyName: '',
    dob: '',
    idNumber: '',
    phoneNumber: '',
    address: '',
    gender: '',
    maritalStatus: '',
    kidsCount: '',
    nextOfKinName: '',
    nextOfKinPhone: '',
    corpsName: '',
    enrolledCorpsName: '',
    ward: '',
    brigade: '',
    favoriteSong: '',
    favoriteBibleVerse: '',
    status: 'IN_PROGRESS',
  });
  
  const [personImage, setPersonImage] = useState(null);
  const [certificateImage, setCertificateImage] = useState(null);
  const [personImagePreview, setPersonImagePreview] = useState('');
  const [certificateImagePreview, setCertificateImagePreview] = useState('');
  
  const [errors, setErrors] = useState({});
  const calculatedAge = useMemo(() => calculateAgeFromDob(formData.dob), [formData.dob]);
  const requiresIdNumber = calculatedAge != null && calculatedAge >= 18;
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    if (recordData) {
      setFormData({
        waId: recordData.waId || '',
        firstName: recordData.firstName || '',
        familyName: recordData.familyName || '',
        dob: recordData.dob || '',
        idNumber: recordData.idNumber || '',
        phoneNumber: recordData.phoneNumber || '',
        address: recordData.homeAddress || recordData.address || '',
        gender: recordData.gender || '',
        maritalStatus: recordData.maritalStatus || '',
        kidsCount: recordData.kidsCount != null && recordData.kidsCount !== '' ? String(recordData.kidsCount) : '',
        nextOfKinName: recordData.nextOfKinName || '',
        nextOfKinPhone: recordData.nextOfKinPhone || '',
        corpsName: DEFAULT_CORPS_NAME,
        enrolledCorpsName: DEFAULT_CORPS_NAME,
        ward: recordData.ward || '',
        brigade: recordData.brigade || '',
        favoriteSong: recordData.favoriteSong || '',
        favoriteBibleVerse: recordData.favoriteBibleVerse || '',
        status: recordData.status || 'IN_PROGRESS',
      });
    } else {
      setFormData({
        waId: '',
        firstName: '',
        familyName: '',
        dob: '',
        idNumber: '',
        phoneNumber: '',
        address: '',
        gender: '',
        maritalStatus: '',
        kidsCount: '',
        nextOfKinName: '',
        nextOfKinPhone: '',
        corpsName: DEFAULT_CORPS_NAME,
        enrolledCorpsName: DEFAULT_CORPS_NAME,
        ward: '',
        brigade: '',
        favoriteSong: '',
        favoriteBibleVerse: '',
        status: 'IN_PROGRESS',
      });
    }
    setPersonImage(null);
    setCertificateImage(null);
    setPersonImagePreview('');
    setCertificateImagePreview('');
  }, [isOpen, recordData]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleFileChange = (e, setImage, setPreview) => {
    const file = e.target.files?.[0] || null;
    setImage(file);
    if (!file) {
      setPreview('');
      return;
    }
    const reader = new FileReader();
    reader.onload = (event) => setPreview(event.target?.result || '');
    reader.readAsDataURL(file);
  };

  const validate = () => {
    const newErrors = {};
    
    if (!formData.waId.trim()) {
      newErrors.waId = 'WhatsApp ID is required';
    }
    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required';
    }
    if (!formData.familyName.trim()) {
      newErrors.familyName = 'Family name is required';
    }
    if (!formData.dob) {
      newErrors.dob = 'Date of birth is required';
    }
    if (requiresIdNumber && !formData.idNumber.trim()) {
      newErrors.idNumber = 'ID Number is required for members 18 years or older';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validate()) return;
    
    setIsSubmitting(true);
    try {
      let personImageUrl = recordData?.personImageUrl; // Keep existing if not updated
      if (personImage) {
        const formDataImage = new FormData();
        formDataImage.append('file', personImage);
        const uploadRes = await http.post('/api/uploads/person-image', formDataImage, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        });
        personImageUrl = uploadRes.data.filename;
      }

      let certificateImageUrl = recordData?.certImageUrl; // Keep existing if not updated
      if (certificateImage) {
        const formDataImage = new FormData();
        formDataImage.append('file', certificateImage);
        const uploadRes = await http.post('/api/uploads/certificate-image', formDataImage, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        });
        certificateImageUrl = uploadRes.data.filename;
      }

      const kidsParsed =
        formData.kidsCount === '' || formData.kidsCount === undefined
          ? null
          : parseInt(formData.kidsCount, 10);
      const kidsCount = Number.isNaN(kidsParsed) ? 0 : kidsParsed;

      const basePayload = {
        ...formData,
        corpsName: DEFAULT_CORPS_NAME,
        enrolledCorpsName: DEFAULT_CORPS_NAME,
        idNumber: requiresIdNumber ? formData.idNumber.trim() : (formData.idNumber.trim() || null),
        dob: formData.dob || null,
        status: formData.status.toUpperCase(),
        gender: formData.gender.trim() || null,
        maritalStatus: formData.maritalStatus.trim() || null,
        kidsCount,
        personImageUrl,
        certificateImageUrl,
      };

      if (isEdit) {
        await onSave(recordData.id, basePayload);
      } else {
        const { personImageUrl: _pi, certificateImageUrl: _ci, ...createBody } = basePayload;
        await onSave(createBody);
      }
      
      setErrors({});
      onClose();
    } catch (error) {
      setErrors({ submit: error.message || 'Failed to save record' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setErrors({});
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-900/40 backdrop-blur-sm animate-fade-in">
      {/* Slide-over Panel */}
      <div className="w-full max-w-lg bg-white h-full shadow-2xl flex flex-col animate-slide-in-right">
        
        {/* Header */}
        <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-surface-50">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">{isEdit ? 'Edit Record' : 'Add Record'}</h2>
            <p className="text-sm text-slate-500 mt-1">
              {isEdit ? 'Modify data collection record' : 'Create a new soldier record'}
            </p>
          </div>
          <button 
            onClick={handleClose} 
            disabled={isSubmitting}
            className="p-2 hover:bg-slate-200 rounded-xl transition-colors disabled:opacity-50"
          >
            <X size={20} className="text-slate-500" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-8">
          
          {/* Global Error */}
          {errors.submit && (
            <div className="bg-red-50 border-2 border-brand-red/20 rounded-xl p-4 flex items-start gap-3 animate-shake">
              <AlertCircle size={20} className="text-brand-red flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-semibold text-brand-red">Error</p>
                <p className="text-sm text-red-700">{errors.submit}</p>
              </div>
            </div>
          )}

          {/* Section: Personal Information */}
          <div className="space-y-4">
            <h3 className="text-xs font-bold text-brand-red uppercase tracking-widest flex items-center gap-2">
              <User size={14} /> Personal Information
            </h3>
            
            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                WhatsApp ID <span className="text-brand-red">*</span>
              </label>
              <input 
                type="text" 
                name="waId"
                value={formData.waId}
                onChange={handleChange}
                className={`w-full px-4 py-2.5 bg-slate-50 border rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all ${
                  errors.waId ? 'border-red-300 bg-red-50' : 'border-slate-200'
                }`}
                placeholder="27821234567" 
              />
              {errors.waId && (
                <p className="text-xs text-red-600 flex items-center gap-1">
                  <AlertCircle size={12} />
                  {errors.waId}
                </p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">
                  First Name <span className="text-brand-red">*</span>
                </label>
                <input 
                  type="text" 
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  className={`w-full px-4 py-2.5 bg-slate-50 border rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all ${
                    errors.firstName ? 'border-red-300 bg-red-50' : 'border-slate-200'
                  }`}
                  placeholder="John" 
                />
                {errors.firstName && (
                  <p className="text-xs text-red-600 flex items-center gap-1">
                    <AlertCircle size={12} />
                    {errors.firstName}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">
                  Family Name <span className="text-brand-red">*</span>
                </label>
                <input 
                  type="text" 
                  name="familyName"
                  value={formData.familyName}
                  onChange={handleChange}
                  className={`w-full px-4 py-2.5 bg-slate-50 border rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all ${
                    errors.familyName ? 'border-red-300 bg-red-50' : 'border-slate-200'
                  }`}
                  placeholder="Doe" 
                />
                {errors.familyName && (
                  <p className="text-xs text-red-600 flex items-center gap-1">
                    <AlertCircle size={12} />
                    {errors.familyName}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Date of Birth <span className="text-brand-red">*</span>
              </label>
              <input 
                type="date" 
                name="dob"
                value={formData.dob}
                onChange={handleChange}
                className={`w-full px-4 py-2.5 bg-slate-50 border rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all ${
                  errors.dob ? 'border-red-300 bg-red-50' : 'border-slate-200'
                }`}
              />
              {errors.dob && (
                <p className="text-xs text-red-600 flex items-center gap-1">
                  <AlertCircle size={12} />
                  {errors.dob}
                </p>
              )}
              <p className="text-xs text-slate-500">Age will be calculated automatically</p>
            </div>

            {requiresIdNumber && (
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">
                  ID Number <span className="text-brand-red">*</span>
                </label>
                <input 
                  type="text" 
                  name="idNumber"
                  value={formData.idNumber}
                  onChange={handleChange}
                  className={`w-full px-4 py-2.5 bg-slate-50 border rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all ${
                    errors.idNumber ? 'border-red-300 bg-red-50' : 'border-slate-200'
                  }`}
                  placeholder="Enter ID number" 
                />
                {errors.idNumber && (
                  <p className="text-xs text-red-600 flex items-center gap-1">
                    <AlertCircle size={12} />
                    {errors.idNumber}
                  </p>
                )}
              </div>
            )}

            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">Primary Contact Number</label>
              <input
                type="text"
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all"
                placeholder="e.g. +263..."
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">Home Address</label>
              <textarea
                name="address"
                value={formData.address}
                onChange={handleChange}
                rows="2"
                className="w-full max-w-full box-border px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all resize-y"
                placeholder="Enter home address"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">Gender</label>
                <select
                  name="gender"
                  value={formData.gender}
                  onChange={handleChange}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all"
                >
                  <option value="">Not specified</option>
                  <option value="Female">Female</option>
                  <option value="Male">Male</option>
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">Marital status</label>
                <select
                  name="maritalStatus"
                  value={formData.maritalStatus}
                  onChange={handleChange}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all"
                >
                  <option value="">Not specified</option>
                  <option value="Single">Single</option>
                  <option value="Married">Married</option>
                  <option value="Divorced">Divorced</option>
                  <option value="Widow">Widow</option>
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">Children (count)</label>
                <input
                  type="number"
                  min="0"
                  name="kidsCount"
                  value={formData.kidsCount}
                  onChange={handleChange}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all"
                  placeholder="0"
                />
              </div>
            </div>
            <p className="text-xs text-slate-500 -mt-2">
              Used to assign fellowship (Home League, Men&apos;s Fellowship, Youth, etc.)
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">Next of Kin Name</label>
                <input
                  type="text"
                  name="nextOfKinName"
                  value={formData.nextOfKinName}
                  onChange={handleChange}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all"
                  placeholder="Next of kin full name"
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-700">Next of Kin Phone</label>
                <input
                  type="text"
                  name="nextOfKinPhone"
                  value={formData.nextOfKinPhone}
                  onChange={handleChange}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all"
                  placeholder="Next of kin contact"
                />
              </div>
            </div>
          </div>

          {/* Section: Organization */}
          <div className="space-y-4 pt-6 border-t border-slate-100">
            <h3 className="text-xs font-bold text-brand-red uppercase tracking-widest flex items-center gap-2">
              <Building2 size={14} /> Organization Details
            </h3>
            
            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Ward <span className="text-slate-400 font-normal">(Optional)</span>
              </label>
              <input 
                type="text" 
                name="ward"
                value={formData.ward}
                onChange={handleChange}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all" 
                placeholder="Enter Ward" 
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Brigade <span className="text-slate-400 font-normal">(Optional)</span>
              </label>
              <input 
                type="text" 
                name="brigade"
                value={formData.brigade}
                onChange={handleChange}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all" 
                placeholder="Enter Brigade" 
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Favorite Song <span className="text-slate-400 font-normal">(Optional)</span>
              </label>
              <textarea 
                name="favoriteSong"
                value={formData.favoriteSong}
                onChange={handleChange}
                rows="2"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all resize-none" 
                placeholder="Enter favorite hymn or song" 
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Favorite Bible Verse <span className="text-slate-400 font-normal">(Optional)</span>
              </label>
              <textarea 
                name="favoriteBibleVerse"
                value={formData.favoriteBibleVerse}
                onChange={handleChange}
                rows="2"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all resize-none" 
                placeholder="Enter favorite Bible verse" 
              />
            </div>
          </div>

          {/* Section: Images */}
          <div className="space-y-4 pt-6 border-t border-slate-100">
            <h3 className="text-xs font-bold text-brand-red uppercase tracking-widest flex items-center gap-2">
              <Image size={14} /> Images
            </h3>
            
            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Person Image <span className="text-slate-400 font-normal">(Optional)</span>
              </label>
              <input 
                type="file" 
                name="personImage"
                onChange={(e) => handleFileChange(e, setPersonImage, setPersonImagePreview)}
                className={`w-full px-4 py-2.5 bg-slate-50 border rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all ${
                  errors.personImage ? 'border-red-300 bg-red-50' : 'border-slate-200'
                }`}
                accept="image/*" // Accept only image files
              />
              {personImagePreview && (
                <img
                  src={personImagePreview}
                  alt="Selected profile preview"
                  className="mt-2 w-full max-h-44 object-cover rounded-lg border border-slate-200"
                />
              )}
              {errors.personImage && (
                <p className="text-xs text-red-600 flex items-center gap-1">
                  <AlertCircle size={12} />
                  {errors.personImage}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-slate-700">
                Certificate Image <span className="text-slate-400 font-normal">(Optional)</span>
              </label>
              <input 
                type="file" 
                name="certificateImage"
                onChange={(e) => handleFileChange(e, setCertificateImage, setCertificateImagePreview)}
                className={`w-full px-4 py-2.5 bg-slate-50 border rounded-xl focus:ring-2 focus:ring-brand-red/20 focus:border-brand-red outline-none transition-all ${
                  errors.certificateImage ? 'border-red-300 bg-red-50' : 'border-slate-200'
                }`}
                accept="image/*" // Accept only image files
              />
              {certificateImagePreview && (
                <img
                  src={certificateImagePreview}
                  alt="Selected certificate preview"
                  className="mt-2 w-full max-h-44 object-cover rounded-lg border border-slate-200"
                />
              )}
              {errors.certificateImage && (
                <p className="text-xs text-red-600 flex items-center gap-1">
                  <AlertCircle size={12} />
                  {errors.certificateImage}
                </p>
              )}
            </div>
          </div>

          {/* Section: Status */}
          <div className="space-y-4 pt-6 border-t border-slate-100">
            <h3 className="text-xs font-bold text-brand-red uppercase tracking-widest flex items-center gap-2">
              <ShieldCheck size={14} /> Record Status
            </h3>
            
            <div className="space-y-3">
              <label className="flex items-center gap-3 p-4 border-2 border-slate-200 rounded-xl cursor-pointer hover:bg-slate-50 transition-all has-[:checked]:border-emerald-500 has-[:checked]:bg-emerald-50">
                <input 
                  type="radio" 
                  name="status" 
                  value="VERIFIED"
                  checked={formData.status === 'VERIFIED'}
                  onChange={handleChange}
                  className="w-4 h-4 text-emerald-600 focus:ring-emerald-500" 
                />
                <div className="flex-1">
                  <span className="text-sm text-slate-900 font-semibold">Verified</span>
                  <p className="text-xs text-slate-500 mt-0.5">All information verified and record is complete</p>
                </div>
              </label>
              
              <label className="flex items-center gap-3 p-4 border-2 border-slate-200 rounded-xl cursor-pointer hover:bg-slate-50 transition-all has-[:checked]:border-amber-500 has-[:checked]:bg-amber-50">
                <input 
                  type="radio" 
                  name="status" 
                  value="IN_PROGRESS"
                  checked={formData.status === 'IN_PROGRESS'}
                  onChange={handleChange}
                  className="w-4 h-4 text-amber-600 focus:ring-amber-500" 
                />
                <div className="flex-1">
                  <span className="text-sm text-slate-900 font-semibold">In Progress</span>
                  <p className="text-xs text-slate-500 mt-0.5">Record needs additional information or verification</p>
                </div>
              </label>
            </div>
          </div>
        </form>

        {/* Footer Actions */}
        <div className="p-6 border-t border-slate-100 bg-surface-50 flex gap-3">
          <button 
            type="button"
            onClick={handleClose} 
            disabled={isSubmitting}
            className="flex-1 py-3 border-2 border-slate-200 text-slate-700 font-semibold rounded-xl hover:bg-white hover:border-slate-300 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Cancel
          </button>
          <button 
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="flex-1 py-3 bg-brand-red text-white font-semibold rounded-xl hover:bg-brand-redDark shadow-lg shadow-red-900/20 flex items-center justify-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? (
              <>
                <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Saving...
              </>
            ) : (
              <>
                <Save size={18} />
                {isEdit ? 'Save Record' : 'Create Record'}
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default EditRecordModal;
