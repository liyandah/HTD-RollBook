import React, { useState } from 'react';
import { 
  Settings as SettingsIcon, Shield, Bell, Database, Save, 
  Globe, Lock, Key, AlertTriangle, CheckCircle, Info,
  Mail, User, Building2, Clock, HardDrive, Activity
} from 'lucide-react';
import Toast from '../components/common/Toast';

const Settings = () => {
  const [activeTab, setActiveTab] = useState('general');
  const [toast, setToast] = useState(null);
  const [formData, setFormData] = useState({
    organizationName: 'HT-E Roll Book',
    supportEmail: 'admin@salvationarmy.org',
    language: 'en-GB',
    timezone: 'Africa/Johannesburg',
    twoFactorEnabled: false,
    sessionTimeout: '30',
    passwordExpiry: '90',
    emailNotifications: true,
    systemNotifications: true,
    weeklyReports: true,
  });

  const navItems = [
    { id: 'general', label: 'General', icon: SettingsIcon, description: 'Organization settings' },
    { id: 'security', label: 'Security', icon: Shield, description: 'Access & authentication' },
    { id: 'notifications', label: 'Notifications', icon: Bell, description: 'Alerts & emails' },
    { id: 'system', label: 'System Info', icon: Database, description: 'Performance & logs' },
  ];

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSave = (section) => {
    // Simulate API call
    setTimeout(() => {
      setToast({ 
        message: `${section} settings saved successfully!`, 
        type: 'success' 
      });
    }, 500);
  };

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
          <h1 className="text-3xl font-bold text-slate-900">System Settings</h1>
          <p className="text-slate-500 mt-1">Configure your data collection preferences and security</p>
        </div>
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-xl">
          <CheckCircle size={18} className="text-emerald-600" />
          <span className="text-sm font-medium text-emerald-700">All systems operational</span>
        </div>
      </div>

      {/* Settings Layout */}
      <div className="flex flex-col lg:flex-row gap-6">
        {/* Settings Sidebar */}
        <div className="w-full lg:w-72 space-y-2">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-start gap-3 px-5 py-4 rounded-xl font-medium transition-all text-left ${
                activeTab === item.id 
                  ? 'bg-brand-red text-white shadow-lg shadow-red-900/20' 
                  : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-100'
              }`}
            >
              <item.icon size={20} className={activeTab === item.id ? 'flex-shrink-0 mt-0.5' : 'flex-shrink-0 mt-0.5 text-slate-400'} />
              <div>
                <p className="font-semibold">{item.label}</p>
                <p className={`text-xs mt-0.5 ${activeTab === item.id ? 'text-white/80' : 'text-slate-500'}`}>
                  {item.description}
                </p>
              </div>
            </button>
          ))}
        </div>

        {/* Settings Content Area */}
        <div className="flex-1">
          {/* General Tab */}
          {activeTab === 'general' && (
            <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-8 space-y-8 animate-fadeIn">
              {/* Organization Profile */}
              <div>
                <div className="flex items-start gap-3 mb-6 pb-6 border-b border-slate-100">
                  <div className="p-2 bg-blue-50 rounded-lg">
                    <Building2 size={20} className="text-blue-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">Organization Profile</h3>
                    <p className="text-sm text-slate-500 mt-1">Update the public information for this branch</p>
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-6">
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                      <Building2 size={14} className="text-slate-400" />
                      Organization Name
                    </label>
                    <input 
                      type="text" 
                      name="organizationName"
                      value={formData.organizationName}
                      onChange={handleChange}
                      className="input-field"
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                      <Mail size={14} className="text-slate-400" />
                      Support Email
                    </label>
                    <input 
                      type="email" 
                      name="supportEmail"
                      value={formData.supportEmail}
                      onChange={handleChange}
                      className="input-field"
                      placeholder="admin@salvationarmy.org"
                    />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                        <Globe size={14} className="text-slate-400" />
                        Language / Region
                      </label>
                      <select 
                        name="language"
                        value={formData.language}
                        onChange={handleChange}
                        className="input-field"
                      >
                        <option value="en-GB">English (United Kingdom)</option>
                        <option value="en-US">English (United States)</option>
                        <option value="en-ZA">English (South Africa)</option>
                      </select>
                    </div>

                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                        <Clock size={14} className="text-slate-400" />
                        Timezone
                      </label>
                      <select 
                        name="timezone"
                        value={formData.timezone}
                        onChange={handleChange}
                        className="input-field"
                      >
                        <option value="Africa/Johannesburg">Africa/Johannesburg (UTC+2)</option>
                        <option value="Europe/London">Europe/London (UTC+0)</option>
                        <option value="America/New_York">America/New York (UTC-5)</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="pt-6 flex justify-end">
                  <button 
                    onClick={() => handleSave('General')}
                    className="btn-primary flex items-center gap-2"
                  >
                    <Save size={18} />
                    Save Changes
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Security Tab */}
          {activeTab === 'security' && (
            <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-8 space-y-8 animate-fadeIn">
              {/* Authentication Settings */}
              <div>
                <div className="flex items-start gap-3 mb-6 pb-6 border-b border-slate-100">
                  <div className="p-2 bg-red-50 rounded-lg">
                    <Shield size={20} className="text-brand-red" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">Authentication & Access</h3>
                    <p className="text-sm text-slate-500 mt-1">Manage security and access control settings</p>
                  </div>
                </div>

                <div className="space-y-6">
                  {/* Two-Factor Authentication */}
                  <div className="p-5 bg-slate-50 border border-slate-200 rounded-xl">
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3">
                        <Lock size={20} className="text-slate-600 mt-1" />
                        <div>
                          <h4 className="font-semibold text-slate-900">Two-Factor Authentication</h4>
                          <p className="text-sm text-slate-600 mt-1">Add an extra layer of security to your account</p>
                        </div>
                      </div>
                      <button
                        onClick={() => handleChange({ target: { name: 'twoFactorEnabled', type: 'checkbox', checked: !formData.twoFactorEnabled }})}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-brand-red focus:ring-offset-2 ${
                          formData.twoFactorEnabled ? 'bg-brand-red' : 'bg-slate-300'
                        }`}
                      >
                        <span
                          className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                            formData.twoFactorEnabled ? 'translate-x-6' : 'translate-x-1'
                          }`}
                        />
                      </button>
                    </div>
                  </div>

                  {/* Session Settings */}
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                        <Clock size={14} className="text-slate-400" />
                        Session Timeout (minutes)
                      </label>
                      <input 
                        type="number" 
                        name="sessionTimeout"
                        value={formData.sessionTimeout}
                        onChange={handleChange}
                        className="input-field"
                        min="5"
                        max="120"
                      />
                      <p className="text-xs text-slate-500">Users will be logged out after this period of inactivity</p>
                    </div>

                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-700 flex items-center gap-2">
                        <Key size={14} className="text-slate-400" />
                        Password Expiry (days)
                      </label>
                      <input 
                        type="number" 
                        name="passwordExpiry"
                        value={formData.passwordExpiry}
                        onChange={handleChange}
                        className="input-field"
                        min="30"
                        max="365"
                      />
                      <p className="text-xs text-slate-500">Users must change their password after this many days</p>
                    </div>
                  </div>

                  {/* Security Alert */}
                  <div className="p-4 bg-amber-50 border-2 border-amber-200 rounded-xl flex items-start gap-3">
                    <AlertTriangle size={20} className="text-amber-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="text-sm font-semibold text-amber-900">Security Recommendation</p>
                      <p className="text-sm text-amber-700 mt-1">
                        Enable two-factor authentication for all admin users to enhance security
                      </p>
                    </div>
                  </div>
                </div>

                <div className="pt-6 flex justify-end">
                  <button 
                    onClick={() => handleSave('Security')}
                    className="btn-primary flex items-center gap-2"
                  >
                    <Save size={18} />
                    Save Security Settings
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-8 space-y-8 animate-fadeIn">
              <div>
                <div className="flex items-start gap-3 mb-6 pb-6 border-b border-slate-100">
                  <div className="p-2 bg-purple-50 rounded-lg">
                    <Bell size={20} className="text-purple-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">Notification Preferences</h3>
                    <p className="text-sm text-slate-500 mt-1">Control how and when you receive updates</p>
                  </div>
                </div>

                <div className="space-y-4">
                  {/* Email Notifications */}
                  <div className="p-5 bg-slate-50 border border-slate-200 rounded-xl">
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3">
                        <Mail size={20} className="text-slate-600 mt-1" />
                        <div>
                          <h4 className="font-semibold text-slate-900">Email Notifications</h4>
                          <p className="text-sm text-slate-600 mt-1">Receive important updates via email</p>
                        </div>
                      </div>
                      <button
                        onClick={() => handleChange({ target: { name: 'emailNotifications', type: 'checkbox', checked: !formData.emailNotifications }})}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-brand-red focus:ring-offset-2 ${
                          formData.emailNotifications ? 'bg-emerald-500' : 'bg-slate-300'
                        }`}
                      >
                        <span
                          className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                            formData.emailNotifications ? 'translate-x-6' : 'translate-x-1'
                          }`}
                        />
                      </button>
                    </div>
                  </div>

                  {/* System Notifications */}
                  <div className="p-5 bg-slate-50 border border-slate-200 rounded-xl">
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3">
                        <Bell size={20} className="text-slate-600 mt-1" />
                        <div>
                          <h4 className="font-semibold text-slate-900">System Notifications</h4>
                          <p className="text-sm text-slate-600 mt-1">Get notified about system events and updates</p>
                        </div>
                      </div>
                      <button
                        onClick={() => handleChange({ target: { name: 'systemNotifications', type: 'checkbox', checked: !formData.systemNotifications }})}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-brand-red focus:ring-offset-2 ${
                          formData.systemNotifications ? 'bg-emerald-500' : 'bg-slate-300'
                        }`}
                      >
                        <span
                          className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                            formData.systemNotifications ? 'translate-x-6' : 'translate-x-1'
                          }`}
                        />
                      </button>
                    </div>
                  </div>

                  {/* Weekly Reports */}
                  <div className="p-5 bg-slate-50 border border-slate-200 rounded-xl">
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-3">
                        <Activity size={20} className="text-slate-600 mt-1" />
                        <div>
                          <h4 className="font-semibold text-slate-900">Weekly Summary Reports</h4>
                          <p className="text-sm text-slate-600 mt-1">Receive a weekly summary of your data collection</p>
                        </div>
                      </div>
                      <button
                        onClick={() => handleChange({ target: { name: 'weeklyReports', type: 'checkbox', checked: !formData.weeklyReports }})}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-brand-red focus:ring-offset-2 ${
                          formData.weeklyReports ? 'bg-emerald-500' : 'bg-slate-300'
                        }`}
                      >
                        <span
                          className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                            formData.weeklyReports ? 'translate-x-6' : 'translate-x-1'
                          }`}
                        />
                      </button>
                    </div>
                  </div>
                </div>

                <div className="pt-6 flex justify-end">
                  <button 
                    onClick={() => handleSave('Notification')}
                    className="btn-primary flex items-center gap-2"
                  >
                    <Save size={18} />
                    Save Preferences
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* System Info Tab */}
          {activeTab === 'system' && (
            <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-8 space-y-8 animate-fadeIn">
              <div>
                <div className="flex items-start gap-3 mb-6 pb-6 border-b border-slate-100">
                  <div className="p-2 bg-emerald-50 rounded-lg">
                    <Database size={20} className="text-emerald-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">System Information</h3>
                    <p className="text-sm text-slate-500 mt-1">Performance metrics and system logs</p>
                  </div>
                </div>

                {/* System Stats */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                  <div className="p-6 bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl border border-blue-200">
                    <div className="flex items-center gap-3 mb-3">
                      <Database size={24} className="text-blue-600" />
                      <h4 className="font-semibold text-blue-900">Database</h4>
                    </div>
                    <p className="text-3xl font-bold text-blue-900">98.5%</p>
                    <p className="text-sm text-blue-700 mt-1">Health Status</p>
                  </div>

                  <div className="p-6 bg-gradient-to-br from-emerald-50 to-emerald-100 rounded-xl border border-emerald-200">
                    <div className="flex items-center gap-3 mb-3">
                      <HardDrive size={24} className="text-emerald-600" />
                      <h4 className="font-semibold text-emerald-900">Storage</h4>
                    </div>
                    <p className="text-3xl font-bold text-emerald-900">2.4 GB</p>
                    <p className="text-sm text-emerald-700 mt-1">Used of 10 GB</p>
                  </div>

                  <div className="p-6 bg-gradient-to-br from-purple-50 to-purple-100 rounded-xl border border-purple-200">
                    <div className="flex items-center gap-3 mb-3">
                      <Activity size={24} className="text-purple-600" />
                      <h4 className="font-semibold text-purple-900">API Calls</h4>
                    </div>
                    <p className="text-3xl font-bold text-purple-900">1,284</p>
                    <p className="text-sm text-purple-700 mt-1">Last 24 hours</p>
                  </div>

                  <div className="p-6 bg-gradient-to-br from-amber-50 to-amber-100 rounded-xl border border-amber-200">
                    <div className="flex items-center gap-3 mb-3">
                      <Clock size={24} className="text-amber-600" />
                      <h4 className="font-semibold text-amber-900">Uptime</h4>
                    </div>
                    <p className="text-3xl font-bold text-amber-900">99.9%</p>
                    <p className="text-sm text-amber-700 mt-1">Last 30 days</p>
                  </div>
                </div>

                {/* System Details */}
                <div className="space-y-3">
                  <h4 className="font-semibold text-slate-900 mb-4">System Details</h4>
                  
                  <div className="flex justify-between items-center p-4 bg-slate-50 rounded-lg">
                    <span className="text-sm font-medium text-slate-600">Application Version</span>
                    <span className="text-sm font-semibold text-slate-900">1.0.0</span>
                  </div>

                  <div className="flex justify-between items-center p-4 bg-slate-50 rounded-lg">
                    <span className="text-sm font-medium text-slate-600">Database Version</span>
                    <span className="text-sm font-semibold text-slate-900">PostgreSQL 15.2</span>
                  </div>

                  <div className="flex justify-between items-center p-4 bg-slate-50 rounded-lg">
                    <span className="text-sm font-medium text-slate-600">Backend Framework</span>
                    <span className="text-sm font-semibold text-slate-900">Spring Boot 3.2.1</span>
                  </div>

                  <div className="flex justify-between items-center p-4 bg-slate-50 rounded-lg">
                    <span className="text-sm font-medium text-slate-600">Last Backup</span>
                    <span className="text-sm font-semibold text-slate-900">2 hours ago</span>
                  </div>
                </div>

                {/* Info Box */}
                <div className="mt-6 p-4 bg-blue-50 border-2 border-blue-200 rounded-xl flex items-start gap-3">
                  <Info size={20} className="text-blue-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="text-sm font-semibold text-blue-900">System Maintenance</p>
                    <p className="text-sm text-blue-700 mt-1">
                      Automatic backups run daily at 2:00 AM. Database optimization occurs weekly.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Settings;






