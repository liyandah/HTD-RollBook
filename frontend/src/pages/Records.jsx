import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Search, Download, Filter, ChevronLeft, ChevronRight, FileText, Eye, Plus, Edit, Trash2, Upload, X } from 'lucide-react';
import * as XLSX from 'xlsx';
import http from '../api/apiClient';
import Badge from '../components/common/Badge';
import LoadingSkeleton from '../components/common/LoadingSkeleton';
import Toast from '../components/common/Toast';
import AddRecordModal from '../components/modals/AddRecordModal';
import { getDepartmentTagClass } from '../utils/departmentTags';
import { getCorpsDisplayName } from '../utils/corpsName';

const Records = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filters, setFilters] = useState({
    status: '',
    from: '',
    to: '',
    q: '',
    department: '',
  });
  const [bulkValidateResult, setBulkValidateResult] = useState(null);
  const [bulkWarningApproved, setBulkWarningApproved] = useState({});
  const [toast, setToast] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [bulkImportLoading, setBulkImportLoading] = useState(false);
  const [bulkScanLoading, setBulkScanLoading] = useState(false);
  const [bulkImportModalOpen, setBulkImportModalOpen] = useState(false);
  const [bulkImportPayload, setBulkImportPayload] = useState([]);
  const fileInputRef = useRef(null);

  const householdRows = useMemo(() => {
    if (!records?.length) return [];
    const byRecordCode = new Map();
    const byNationalId = new Map();
    for (const r of records) {
      if (r.recordCode) byRecordCode.set(String(r.recordCode).trim().toLowerCase(), r);
      if (r.idNumber) byNationalId.set(String(r.idNumber).trim().toLowerCase(), r);
    }
    const resolveParent = (proxyVal) => {
      if (!proxyVal) return null;
      const key = String(proxyVal).trim().toLowerCase();
      return byRecordCode.get(key) || byNationalId.get(key) || null;
    };
    const map = new Map();
    for (const r of records) {
      let k;
      if (r.proxyId) {
        const p = resolveParent(r.proxyId);
        k = p ? String(p.id) : `proxy:${String(r.proxyId)}`;
      } else {
        k = String(r.proxyId || r.idNumber || r.id);
      }
      if (!map.has(k)) map.set(k, []);
      map.get(k).push(r);
    }
    const out = [];
    let firstGroup = true;
    for (const [k, members] of map) {
      const hasHousehold = members.some((m) => m.proxyId);
      const verifyNationalId = hasHousehold
        ? members.find((m) => !m.proxyId && m.idNumber)?.idNumber || members.find((m) => m.proxyId)?.proxyId
        : null;
      const needsVerify = members.some((m) => String(m.status || '').toUpperCase() !== 'VERIFIED');
      const sorted = [...members].sort((a, b) => {
        const aHead =
          !a.proxyId && ((a.idNumber && String(a.idNumber) === k) || String(a.id) === k);
        const bHead =
          !b.proxyId && ((b.idNumber && String(b.idNumber) === k) || String(b.id) === k);
        if (aHead !== bHead) return aHead ? -1 : 1;
        return `${a.firstName || ''} ${a.familyName || ''}`.localeCompare(
          `${b.firstName || ''} ${b.familyName || ''}`
        );
      });
      const groupTopClass = firstGroup ? '' : 'border-t-2 border-slate-200';
      firstGroup = false;
      sorted.forEach((record, idx) => {
        out.push({
          record,
          rowKey: record.id,
          isDependent: Boolean(record.proxyId),
          groupTopClass: idx === 0 ? groupTopClass : '',
          showHouseholdButton: hasHousehold && needsVerify && idx === 0,
          verifyNationalId,
        });
      });
    }
    return out;
  }, [records]);

  const handleVerifyHousehold = async (proxyNationalId) => {
    if (!proxyNationalId) return;
    if (!window.confirm('Verify all members of this household?')) return;
    try {
      await http.post('/api/admin/verify-bulk', {
        proxy_id: proxyNationalId,
        verify_status: 'Verified',
        admin_notes: 'Verified as family unit by Admin',
      });
      setToast({ message: 'Household verified successfully.', type: 'success' });
      fetchRecords();
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to verify household';
      setToast({ message, type: 'error' });
    }
  };

  useEffect(() => {
    fetchRecords();
  }, [page, filters]);

  const fetchRecords = async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams({
        page: page.toString(),
        size: '20',
        ...Object.fromEntries(Object.entries(filters).filter(([_, v]) => v !== '')),
      });

      const response = await http.get(`/api/records?${params.toString()}`);

      const recordsData = response.data?.content || response.data || [];
      const totalPagesData = response.data?.totalPages || 0;

      setRecords(Array.isArray(recordsData) ? recordsData : []);
      setTotalPages(totalPagesData || 0);
    } catch (err) {
      console.error('Failed to load records:', err);
      setToast({ message: 'Failed to load records', type: 'error' });
      setRecords([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
    setPage(0);
  };

  const handleExport = async () => {
    try {
      const params = new URLSearchParams(
        Object.fromEntries(Object.entries(filters).filter(([_, v]) => v !== ''))
      );

      const response = await http.get(`/api/records/export.csv?${params.toString()}`, {
        responseType: 'blob',
      });

      const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'soldier_records.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      setToast({ message: 'CSV exported successfully', type: 'success' });
    } catch (err) {
      console.error('Failed to export:', err);
      setToast({ message: 'Failed to export records', type: 'error' });
    }
  };

  const getCellValue = (row, keys) => {
    for (const key of keys) {
      if (row[key] !== undefined && row[key] !== null && String(row[key]).trim() !== '') {
        return row[key];
      }
    }
    return '';
  };

  const toIsoDate = (value) => {
    if (!value) return null;
    if (typeof value === 'number') {
      const parsed = XLSX.SSF.parse_date_code(value);
      if (!parsed) return null;
      const mm = String(parsed.m).padStart(2, '0');
      const dd = String(parsed.d).padStart(2, '0');
      return `${parsed.y}-${mm}-${dd}`;
    }
    const parsedDate = new Date(value);
    if (!Number.isNaN(parsedDate.getTime())) {
      return parsedDate.toISOString().slice(0, 10);
    }
    return String(value).trim();
  };

  const newSyntheticWaId = (rowIndex) => {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return `import-${crypto.randomUUID().replace(/-/g, '').slice(0, 20)}`;
    }
    return `import-${Date.now()}-${rowIndex}`;
  };

  const parseExcelRows = (rows) => {
    const mapped = rows.map((row, rowIndex) => {
      const kidsRaw = getCellValue(row, [
        'kidsCount',
        'Kids',
        'Kids Count',
        'Children',
        'Number of Children',
        'kids',
      ]);
      let kidsCount = null;
      if (kidsRaw !== '' && kidsRaw != null) {
        const n = parseInt(String(kidsRaw).trim(), 10);
        kidsCount = Number.isNaN(n) ? 0 : n;
      }
      let waId = String(getCellValue(row, ['waId', 'WA ID', 'wa_id', 'whatsapp', 'WhatsApp ID'])).trim();
      if (!waId) {
        waId = newSyntheticWaId(rowIndex);
      }
      return {
        waId,
        firstName: String(getCellValue(row, ['firstName', 'First Name', 'first_name'])).trim(),
        familyName: String(getCellValue(row, ['familyName', 'Family Name', 'Surname', 'Last Name', 'last_name'])).trim(),
        dob: toIsoDate(getCellValue(row, ['dob', 'DOB', 'Date of Birth', 'dateOfBirth'])),
        idNumber: String(getCellValue(row, ['idNumber', 'ID Number', 'id_number'])).trim() || null,
        gender: String(getCellValue(row, ['gender', 'Gender'])).trim() || null,
        maritalStatus: String(getCellValue(row, ['maritalStatus', 'Marital Status', 'marital_status'])).trim() || null,
        kidsCount,
        corpsName: String(getCellValue(row, ['corpsName', 'Corps', 'Corps Name'])).trim() || null,
        enrolledCorpsName: String(getCellValue(row, ['enrolledCorpsName', 'Enrolled Corps', 'Enrolled Corps Name'])).trim() || null,
        ward: String(getCellValue(row, ['ward', 'Ward'])).trim() || null,
        brigade: String(getCellValue(row, ['brigade', 'Brigade'])).trim() || null,
        favoriteSong: String(getCellValue(row, ['favoriteSong', 'Favorite Song'])).trim() || null,
        favoriteBibleVerse: String(getCellValue(row, ['favoriteBibleVerse', 'Favorite Bible Verse', 'Bible Verse'])).trim() || null,
      };
    });

    return mapped.filter((r) => r.firstName && r.familyName && r.dob);
  };

  const handleExcelFileSelected = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      const name = file.name.toLowerCase();
      let workbook;
      if (name.endsWith('.csv')) {
        const text = await file.text();
        workbook = XLSX.read(text, { type: 'string', raw: false });
      } else {
        const buffer = await file.arrayBuffer();
        workbook = XLSX.read(new Uint8Array(buffer), { type: 'array' });
      }
      const firstSheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[firstSheetName];
      const rawRows = XLSX.utils.sheet_to_json(worksheet, { defval: '' });
      const recordsPayload = parseExcelRows(rawRows);

      if (recordsPayload.length === 0) {
        setToast({
          message:
            'No valid rows found. Each row needs First Name, Family Name, and Date of Birth. If WhatsApp ID is empty (e.g. CSV export), a placeholder is assigned automatically.',
          type: 'error',
        });
        return;
      }

      setBulkScanLoading(true);
      setBulkImportPayload(recordsPayload);
      const validateRes = await http.post('/api/records/bulk-validate', recordsPayload);
      setBulkValidateResult(validateRes.data || { blocked: [], warnings: [], clean: [] });
      setBulkWarningApproved({});
      setBulkImportModalOpen(true);
    } catch (err) {
      console.error('Failed to read or validate Excel:', err);
      setToast({ message: err.response?.data?.message || 'Could not read or validate Excel file', type: 'error' });
      setBulkImportPayload([]);
      setBulkValidateResult(null);
    } finally {
      setBulkScanLoading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const cancelBulkImport = () => {
    setBulkImportModalOpen(false);
    setBulkImportPayload([]);
    setBulkValidateResult(null);
    setBulkWarningApproved({});
  };

  const toggleWarningRow = (index) => {
    setBulkWarningApproved((prev) => ({ ...prev, [index]: !prev[index] }));
  };

  const confirmBulkImport = async () => {
    if (!bulkValidateResult) return;

    const cleanRows = (bulkValidateResult.clean || []).map((item) => item.row);
    const approvedWarnings = (bulkValidateResult.warnings || [])
      .filter((w) => bulkWarningApproved[w.index])
      .map((w) => w.row);
    const toImport = [...cleanRows, ...approvedWarnings];

    if (toImport.length === 0) {
      setToast({
        message: 'No rows selected to import. Approve name-warning rows or fix blocked rows.',
        type: 'error',
      });
      return;
    }

    setBulkImportModalOpen(false);
    setBulkImportLoading(true);
    try {
      const response = await http.post('/api/records/bulk-add', toImport);
      const result = response.data || {};
      const failed = result.failed ?? 0;
      const bd = result.departmentBreakdown || {};
      const deptSummary =
        Object.keys(bd).length > 0
          ? ' Departments: ' +
            Object.entries(bd)
              .map(([k, v]) => `${k} ${v}`)
              .join(', ')
          : '';
      const summary =
        failed > 0
          ? `Imported ${result.created}/${result.total}. Failed: ${failed}.${deptSummary}`
          : `Imported ${result.created} record(s). HTF- IDs, Verified.${deptSummary}`;
      setToast({ message: summary, type: failed > 0 ? 'error' : 'success' });
      setBulkImportPayload([]);
      setBulkValidateResult(null);
      setBulkWarningApproved({});
      fetchRecords();
    } catch (err) {
      console.error('Failed to upload Excel:', err);
      const message = err.response?.data?.message || 'Failed to upload Excel file';
      setToast({ message, type: 'error' });
    } finally {
      setBulkImportLoading(false);
    }
  };

  const handleSaveRecord = async (payload) => {
    try {
      const response = await http.post('/api/records', payload);
      
      // Refresh the records list
      fetchRecords();
      
      setToast({ message: 'Record created successfully!', type: 'success' });
      return response.data;
    } catch (err) {
      console.error('Failed to create record:', err);
      const errorMessage = err.response?.data?.message || 'Failed to create record';
      setToast({ message: errorMessage, type: 'error' });
      throw new Error(errorMessage);
    }
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

      {/* Modern Header with Search & Actions */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Data Records</h1>
          <p className="text-slate-500 mt-1">Manage and verify collected information</p>
        </div>
        
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
            <input 
              type="text" 
              name="q"
              value={filters.q}
              onChange={handleFilterChange}
              placeholder="Search by name or corps..." 
              className="pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-red focus:border-transparent outline-none transition-all w-full sm:w-72"
            />
          </div>
          <div className="flex gap-2">
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls,.csv"
              onChange={handleExcelFileSelected}
              className="hidden"
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={bulkImportLoading || bulkScanLoading || bulkImportModalOpen}
              className="inline-flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2.5 rounded-xl font-semibold transition-all shadow-sm hover:shadow-md disabled:opacity-60 disabled:cursor-not-allowed"
            >
              <Upload size={18} />
              {bulkImportLoading || bulkScanLoading ? 'Working…' : 'Upload Excel / CSV'}
            </button>
            <button
              onClick={handleExport}
              className="inline-flex items-center justify-center gap-2 bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2.5 rounded-xl font-semibold transition-all shadow-sm hover:shadow-md"
            >
              <Download size={18} />
              Export CSV
            </button>
            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center justify-center gap-2 bg-brand-red hover:bg-brand-redDark text-white px-4 py-2.5 rounded-xl font-semibold transition-all shadow-lg shadow-red-900/20"
            >
              <Plus size={18} />
              Add Record
            </button>
          </div>
        </div>
      </div>

      {/* Modern Filters Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
        <div className="flex items-center gap-2 mb-5">
          <Filter size={20} className="text-brand-navy" />
          <h2 className="text-lg font-semibold text-slate-900">Filters</h2>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Status
            </label>
            <select
              name="status"
              value={filters.status}
              onChange={handleFilterChange}
              className="w-full px-4 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-navy focus:border-transparent outline-none transition-all bg-white"
            >
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="VERIFIED">Verified</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Fellowship / Department
            </label>
            <select
              name="department"
              value={filters.department}
              onChange={handleFilterChange}
              className="w-full px-4 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-navy focus:border-transparent outline-none transition-all bg-white"
            >
              <option value="">All departments</option>
              <option value="Cradle Roll">Cradle Roll</option>
              <option value="Junior Soldier">Junior Soldier</option>
              <option value="Youth">Youth</option>
              <option value="Youth (Young Women)">Youth (Young Women)</option>
              <option value="Youth (Young Men)">Youth (Young Men)</option>
              <option value="Home League">Home League</option>
              <option value={"Men's Fellowship"}>Men&apos;s Fellowship</option>
              <option value="Senior Citizen (Old Age)">Senior Citizen (Old Age)</option>
              <option value="Senior Soldier">Senior Soldier</option>
              <option value="General Member">General Member</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              From Date
            </label>
            <input
              type="date"
              name="from"
              value={filters.from}
              onChange={handleFilterChange}
              className="w-full px-4 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-navy focus:border-transparent outline-none transition-all"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              To Date
            </label>
            <input
              type="date"
              name="to"
              value={filters.to}
              onChange={handleFilterChange}
              className="w-full px-4 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-navy focus:border-transparent outline-none transition-all"
            />
          </div>
        </div>
      </div>

      {/* Modern Data Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        {loading ? (
          <div className="p-6">
            <LoadingSkeleton count={5} />
          </div>
        ) : records && records.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200">
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Record ID
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Full Name
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Corps
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Ward
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Brigade
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Department
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Created Date
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      DOB
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Age
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      ID Number
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-4 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {householdRows.map(
                    ({ record, rowKey, isDependent, groupTopClass, showHouseholdButton, verifyNationalId }) => {
                    const corpsDisplayName = getCorpsDisplayName(record.corpsName);
                    return (
                      <tr
                        key={rowKey}
                        className={`${groupTopClass} ${isDependent ? 'bg-slate-100/80' : 'bg-white'} hover:bg-slate-50/50 transition-colors group`}
                      >
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-brand-red">
                          {record.recordCode || 'N/A'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className={`flex items-center gap-2 ${isDependent ? 'pl-6' : ''}`}>
                            {isDependent && (
                              <span className="text-slate-500 select-none" title="Dependent">
                                ↳
                              </span>
                            )}
                            <div className="w-8 h-8 rounded-full bg-brand-navy/10 flex items-center justify-center shrink-0">
                              <span className="text-brand-navy font-semibold text-xs">
                                {record.firstName?.[0]}
                                {record.familyName?.[0]}
                              </span>
                            </div>
                            <div className="min-w-0">
                              <span className="text-sm font-semibold text-slate-900">
                                {record.firstName} {record.familyName}
                              </span>
                              {record.proxyId && record.relationship && (
                                <p className="text-xs text-slate-500 mt-0.5">
                                  Linked ({record.relationship}) · primary record {record.proxyId}
                                </p>
                              )}
                              {record.registeredByName && (
                                <p className="text-xs text-slate-500 mt-0.5">
                                  Registered by: {record.registeredByName}
                                  {record.proxyContact && (
                                    <>
                                      {' · '}
                                      <a href={`tel:${String(record.proxyContact).replace(/\s/g, '')}`} className="text-blue-600 hover:underline">
                                        Proxy contact
                                      </a>
                                    </>
                                  )}
                                </p>
                              )}
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {corpsDisplayName ? (
                            <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-medium bg-blue-50 text-blue-700">
                              {corpsDisplayName}
                            </span>
                          ) : (
                            <span className="text-sm text-slate-400">N/A</span>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-700">
                          {record.ward || 'N/A'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-700">
                          {record.brigade || 'N/A'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-700">
                          <span className={`dept-tag ${getDepartmentTagClass(record.department)}`}>
                            {record.department || 'Not assigned'}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-600">
                          {new Date(record.createdAt).toLocaleDateString('en-US', { 
                            month: 'short', 
                            day: 'numeric', 
                            year: 'numeric' 
                          })}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-700">
                          {record.dob ? new Date(record.dob).toLocaleDateString('en-US', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric'
                          }) : 'N/A'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-700">
                          {record.age || 'N/A'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-700">
                          {record.idNumber || 'N/A'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <Badge status={record.status} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-right">
                          {showHouseholdButton && (
                            <button
                              type="button"
                              onClick={() => handleVerifyHousehold(verifyNationalId)}
                              className="mr-2 inline-flex items-center gap-1.5 text-emerald-700 hover:text-emerald-800 font-semibold text-sm"
                            >
                              Verify household
                            </button>
                          )}
                          <Link
                            to={`/records/${record.id}`}
                            className="inline-flex items-center gap-1.5 text-blue-600 hover:text-blue-700 font-medium text-sm transition-colors group/link"
                          >
                            <Eye size={16} className="group-hover/link:scale-110 transition-transform" />
                            View Details
                          </Link>
                          <Link
                            to={`/records/${record.id}`}
                            className="ml-2 inline-flex items-center gap-1.5 text-indigo-600 hover:text-indigo-700 font-medium text-sm transition-colors group/link"
                          >
                            <Edit size={16} className="group-hover/link:scale-110 transition-transform" />
                            Edit
                          </Link>
                          <Link
                            to={`/records/${record.id}`}
                            className="ml-2 inline-flex items-center gap-1.5 text-red-600 hover:text-red-700 font-medium text-sm transition-colors group/link"
                          >
                            <Trash2 size={16} className="group-hover/link:scale-110 transition-transform" />
                            Delete
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Modern Pagination */}
            <div className="bg-slate-50 px-6 py-4 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="text-sm text-slate-600">
                Showing <span className="font-semibold text-slate-900">{records.length}</span> records
                {totalPages > 1 && (
                  <span className="ml-1">
                    • Page <span className="font-semibold text-slate-900">{page + 1}</span> of{' '}
                    <span className="font-semibold text-slate-900">{totalPages}</span>
                  </span>
                )}
              </div>
              {totalPages > 1 && (
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="inline-flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    <ChevronLeft size={16} />
                    Previous
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="inline-flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    Next
                    <ChevronRight size={16} />
                  </button>
                </div>
              )}
            </div>
          </>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200">
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Record ID
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Full Name
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Corps
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Ward
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Brigade
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Department
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Created Date
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      DOB
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Age
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      ID Number
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-4 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td colSpan="12" className="px-6 py-16 text-center">
                      <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-slate-100 mb-4">
                        <FileText size={24} className="text-slate-400" />
                      </div>
                      <h3 className="text-base font-semibold text-slate-900 mb-2">No records found</h3>
                      <p className="text-sm text-slate-500 max-w-sm mx-auto">
                        {filters.q || filters.status || filters.from || filters.to || filters.department
                          ? 'Try adjusting your filters to see more results' 
                          : 'Records will appear here once data is collected'}
                      </p>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      {/* Bulk import: validation review */}
      {bulkImportModalOpen && bulkValidateResult && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl max-w-5xl w-full max-h-[90vh] flex flex-col border border-slate-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Review bulk import</h2>
                <p className="text-sm text-slate-500 mt-0.5">
                  <span className="font-semibold text-red-700">{(bulkValidateResult.blocked || []).length} blocked</span>
                  {' · '}
                  <span className="font-semibold text-amber-700">{(bulkValidateResult.warnings || []).length} name warnings</span>
                  {' · '}
                  <span className="font-semibold text-emerald-700">{(bulkValidateResult.clean || []).length} ready</span>
                  {' · '}
                  {bulkImportPayload.length} row(s) in file. New records get <span className="font-semibold text-brand-navy">HTF-</span> IDs,{' '}
                  <span className="font-semibold text-emerald-700">Verified</span>, and auto fellowship from Gender / Marital / Kids columns when present.
                </p>
              </div>
              <button
                type="button"
                onClick={cancelBulkImport}
                className="p-2 rounded-lg hover:bg-slate-100 text-slate-500"
                aria-label="Close"
              >
                <X size={20} />
              </button>
            </div>
            <div className="px-6 py-4 overflow-y-auto flex-1 space-y-6">
              {(bulkValidateResult.blocked || []).length > 0 && (
                <div>
                  <p className="text-xs font-bold text-red-800 uppercase tracking-wide mb-2">Blocked — will not import</p>
                  <div className="overflow-x-auto rounded-xl border border-red-200 bg-red-50/40">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="bg-red-100/80 text-left text-xs text-red-900 uppercase">
                          <th className="px-3 py-2">#</th>
                          <th className="px-3 py-2">Name</th>
                          <th className="px-3 py-2">ID</th>
                          <th className="px-3 py-2">Reason</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-red-100">
                        {(bulkValidateResult.blocked || []).map((item) => (
                          <tr key={item.index} className="text-slate-800">
                            <td className="px-3 py-2 font-mono">{item.index}</td>
                            <td className="px-3 py-2">
                              {item.row?.firstName} {item.row?.familyName}
                            </td>
                            <td className="px-3 py-2 font-mono text-xs">{item.row?.idNumber || '—'}</td>
                            <td className="px-3 py-2 text-red-800">{item.message}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {(bulkValidateResult.warnings || []).length > 0 && (
                <div>
                  <p className="text-xs font-bold text-amber-800 uppercase tracking-wide mb-2">
                    Name match — tick &quot;Add anyway&quot; to include (same name already in database)
                  </p>
                  <div className="overflow-x-auto rounded-xl border border-amber-200 bg-amber-50/40">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="bg-amber-100/80 text-left text-xs text-amber-900 uppercase">
                          <th className="px-3 py-2 w-10">Add</th>
                          <th className="px-3 py-2">#</th>
                          <th className="px-3 py-2">Name</th>
                          <th className="px-3 py-2">New ID</th>
                          <th className="px-3 py-2">Note</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-amber-100">
                        {(bulkValidateResult.warnings || []).map((item) => (
                          <tr key={item.index} className="text-slate-800">
                            <td className="px-3 py-2">
                              <input
                                type="checkbox"
                                checked={Boolean(bulkWarningApproved[item.index])}
                                onChange={() => toggleWarningRow(item.index)}
                                className="rounded border-slate-300 text-brand-red focus:ring-brand-red"
                              />
                            </td>
                            <td className="px-3 py-2 font-mono">{item.index}</td>
                            <td className="px-3 py-2">
                              {item.row?.firstName} {item.row?.familyName}
                            </td>
                            <td className="px-3 py-2 font-mono text-xs">{item.row?.idNumber || '—'}</td>
                            <td className="px-3 py-2 text-amber-900 text-xs">{item.message}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {(bulkValidateResult.clean || []).length > 0 && (
                <div>
                  <p className="text-xs font-bold text-emerald-800 uppercase tracking-wide mb-2">
                    Ready to import ({(bulkValidateResult.clean || []).length})
                  </p>
                  <div className="overflow-x-auto rounded-xl border border-emerald-200 bg-emerald-50/30">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="bg-emerald-100/80 text-left text-xs text-emerald-900 uppercase">
                          <th className="px-3 py-2">#</th>
                          <th className="px-3 py-2">Name</th>
                          <th className="px-3 py-2">DOB</th>
                          <th className="px-3 py-2">ID</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-emerald-100">
                        {(bulkValidateResult.clean || []).slice(0, 8).map((item) => (
                          <tr key={item.index} className="text-slate-800">
                            <td className="px-3 py-2 font-mono">{item.index}</td>
                            <td className="px-3 py-2">
                              {item.row?.firstName} {item.row?.familyName}
                            </td>
                            <td className="px-3 py-2">{item.row?.dob}</td>
                            <td className="px-3 py-2 font-mono text-xs">{item.row?.idNumber || '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {(bulkValidateResult.clean || []).length > 8 && (
                      <p className="text-xs text-slate-500 px-3 py-2">… and {(bulkValidateResult.clean || []).length - 8} more</p>
                    )}
                  </div>
                </div>
              )}
            </div>
            <div className="flex flex-wrap gap-3 justify-between items-center px-6 py-4 border-t border-slate-100 bg-slate-50/80 rounded-b-2xl">
              <p className="text-sm text-slate-600">
                Importing{' '}
                <span className="font-bold text-slate-900">
                  {(bulkValidateResult.clean || []).length +
                    (bulkValidateResult.warnings || []).filter((w) => bulkWarningApproved[w.index]).length}
                </span>{' '}
                row(s). Blocked rows are skipped.
              </p>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={cancelBulkImport}
                  className="px-4 py-2.5 rounded-xl border border-slate-200 font-semibold text-slate-700 hover:bg-white"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={confirmBulkImport}
                  className="px-4 py-2.5 rounded-xl bg-brand-red text-white font-semibold hover:bg-brand-redDark disabled:opacity-50"
                  disabled={
                    (bulkValidateResult.clean || []).length +
                      (bulkValidateResult.warnings || []).filter((w) => bulkWarningApproved[w.index]).length ===
                    0
                  }
                >
                  Import selected &amp; verify
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {(bulkImportLoading || bulkScanLoading) && (
        <div className="fixed inset-0 z-[60] flex flex-col items-center justify-center bg-slate-900/60 backdrop-blur-sm">
          <div className="w-14 h-14 border-4 border-white border-t-transparent rounded-full animate-spin mb-4" />
          <p className="text-white font-semibold text-lg">
            {bulkScanLoading ? 'Scanning file and checking duplicates…' : 'Importing records…'}
          </p>
          <p className="text-white/80 text-sm mt-1">
            {bulkScanLoading
              ? 'National ID and name matches are verified against the database'
              : 'Assigning record IDs, fellowship, and marking as verified'}
          </p>
        </div>
      )}

      {/* Add Record Modal */}
      <AddRecordModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveRecord}
      />
    </div>
  );
};

export default Records;

