import React, { useMemo, useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Users,
  Clock,
  CheckCircle,
  UserCheck,
  UserPlus,
  Users2,
  Baby,
  ShieldCheck,
  Award,
  XCircle,
  Search,
  Eye,
  Plus
} from 'lucide-react';
import http from '../api/apiClient';
import Badge from '../components/common/Badge';
import LoadingSkeleton from '../components/common/LoadingSkeleton';
import { getCorpsDisplayName } from '../utils/corpsName';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sectionFilter, setSectionFilter] = useState('');

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  useEffect(() => {
    fetchRecords();
  }, []);

  const fetchDashboardStats = async () => {
    try {
      const response = await http.get('/api/records/dashboard');
      console.log('Dashboard stats API response:', {
        status: response.status,
        data: response.data
      });
      setStats(response.data);
    } catch (err) {
      console.error('Failed to load dashboard stats:', {
        error: err,
        response: err.response,
        message: err.message
      });
      setError('Failed to load dashboard stats');
    }
  };

  const fetchRecords = async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams({
        page: '0',
        size: '200',
      });

      const response = await http.get(`/api/records?${params.toString()}`);
      console.log('Dashboard records API response:', {
        status: response.status,
        data: response.data,
        content: response.data?.content,
        contentLength: response.data?.content?.length || 0
      });
      
      const recordsData = response.data?.content || response.data || [];
      setRecords(Array.isArray(recordsData) ? recordsData : []);
    } catch (err) {
      console.error('Failed to load records:', {
        error: err,
        response: err.response,
        message: err.message,
        url: err.config?.url
      });
      setError('Failed to load records');
      setRecords([]);
    } finally {
      setLoading(false);
    }
  };

  const sectionStats = useMemo(() => {
    const data = records || [];
    const cradle = data.filter((r) => (r.department || '') === 'Cradle Roll').length;
    const junior = data.filter((r) => (r.department || '') === 'Junior Soldier').length;
    const youth = data.filter((r) => String(r.department || '').toLowerCase().includes('youth')).length;
    const homeLeague = data.filter((r) => (r.department || '') === 'Home League').length;
    const mensFellowship = data.filter((r) => (r.department || '') === "Men's Fellowship").length;
    const seniors = data.filter((r) =>
      ['Senior Member (60+)', 'Senior Soldier', 'Senior Citizen (Old Age)'].includes(r.department || '')
    ).length;
    const pending = data.filter((r) => {
      const s = String(r.status || '').toUpperCase();
      return s === 'PENDING';
    }).length;
    const verified = data.filter((r) => String(r.status || '').toUpperCase() === 'VERIFIED').length;
    const declined = data.filter((r) => String(r.status || '').toUpperCase() === 'DECLINED').length;
    const childrenTotal = data.reduce((sum, r) => sum + (r.kidsCount || 0), 0);
    return { cradle, junior, youth, homeLeague, mensFellowship, seniors, pending, verified, declined, childrenTotal };
  }, [records]);

  const pendingQueue = useMemo(
    () =>
      (records || [])
        .filter((r) => {
          const s = String(r.status || '').toUpperCase();
          return s === 'PENDING';
        })
        .slice(0, 8),
    [records]
  );

  const visibleRecords = useMemo(() => {
    if (!sectionFilter) return records;
    return (records || []).filter((r) => (r.department || '') === sectionFilter);
  }, [records, sectionFilter]);

  const approveCandidate = async (id) => {
    try {
      await http.post(`/api/admin/verify/${id}`);
      await Promise.all([fetchDashboardStats(), fetchRecords()]);
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to verify candidate');
    }
  };

  const updateRecordStatus = async (id, status) => {
    try {
      await http.patch(`/api/records/${id}/status`, { status });
      await Promise.all([fetchDashboardStats(), fetchRecords()]);
    } catch (e) {
      setError(e.response?.data?.message || `Failed to mark as ${status.toLowerCase()}`);
    }
  };

  const StatCard = ({ title, value, icon, colorClass, description }) => (
    <div className="bg-white p-4 sm:p-6 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between hover:shadow-md transition-all duration-300 group cursor-pointer">
      <div className="flex-1 min-w-0">
        <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">{title}</p>
        <h3 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-slate-900 mb-1 truncate">{value}</h3>
        {description && <p className="text-xs text-slate-500">{description}</p>}
      </div>
      <div className={`p-3 sm:p-4 rounded-xl ${colorClass} transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3 flex-shrink-0`}>
        {icon}
      </div>
    </div>
  );

  if (loading) {
    return (
      <div className="space-y-8">
        <div className="space-y-2">
          <div className="h-10 w-64 bg-slate-200 rounded animate-pulse"></div>
          <div className="h-6 w-96 bg-slate-100 rounded animate-pulse"></div>
        </div>
        <LoadingSkeleton variant="stat" count={4} />
        <LoadingSkeleton variant="stat" count={2} />
        <LoadingSkeleton variant="table" count={5} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
        {error}
      </div>
    );
  }

  return (
    <div className="space-y-8 w-full min-w-0">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div className="flex-1">
          <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-brand-navy mb-2">HT-E Roll Book Overview</h1>
          <p className="text-slate-500 text-sm sm:text-base lg:text-lg">Section performance, verification workflow, and recent enrollments.</p>
        </div>
      </div>

      <div>
        <h2 className="text-lg font-semibold mb-4 text-gray-700">Registration Status</h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <StatCard
            title="Total Registered"
            value={stats?.totalRecords || records.length || 0}
            icon={<Users className="w-6 h-6 text-blue-600" />}
            colorClass="bg-blue-100"
          />
          <StatCard
            title="Pending Verification"
            value={sectionStats.pending}
            icon={<Clock className="w-6 h-6 text-yellow-600" />}
            colorClass="bg-yellow-100"
          />
          <StatCard
            title="Verified Members"
            value={stats?.verifiedCount || sectionStats.verified}
            icon={<CheckCircle className="w-6 h-6 text-green-600" />}
            colorClass="bg-green-100"
          />
          <StatCard
            title="Declined Registrations"
            value={stats?.declinedCount ?? sectionStats.declined}
            icon={<XCircle className="w-6 h-6 text-red-600" />}
            colorClass="bg-red-100"
            description="Rejected Entries"
          />
        </div>
      </div>

      <div>
        <h2 className="text-lg font-semibold mb-4 text-gray-700">Department Breakdown</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          <StatCard
            title="Home League (Women)"
            value={sectionStats.homeLeague}
            icon={<Users2 className="w-6 h-6 text-red-600" />}
            colorClass="bg-red-50"
          />
          <StatCard
            title="Men's Fellowship"
            value={sectionStats.mensFellowship}
            icon={<UserCheck className="w-6 h-6 text-blue-600" />}
            colorClass="bg-blue-50"
          />
          <StatCard
            title="Senior Members (60+)"
            value={sectionStats.seniors}
            icon={<Award className="w-6 h-6 text-gray-700" />}
            colorClass="bg-gray-100"
            description="Golden Age Members"
          />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <StatCard
            title="Youth"
            value={sectionStats.youth}
            icon={<UserPlus className="w-6 h-6 text-purple-600" />}
            colorClass="bg-purple-50"
            description="Ages 15 - 35"
          />
          <StatCard
            title="Junior Soldiers"
            value={sectionStats.junior}
            icon={<ShieldCheck className="w-6 h-6 text-orange-600" />}
            colorClass="bg-orange-50"
            description="Ages 7 - 14"
          />
          <StatCard
            title="Cradle Roll"
            value={sectionStats.cradle}
            icon={<Baby className="w-6 h-6 text-pink-500" />}
            colorClass="bg-pink-50"
            description="Ages 0 - 6"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="xl:col-span-2 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="border-b border-slate-100 px-6 py-4 flex items-center justify-between">
            <h3 className="text-lg font-bold text-slate-900">Recent Enrollments</h3>
            <select
              value={sectionFilter}
              onChange={(e) => setSectionFilter(e.target.value)}
              className="border border-slate-200 rounded-lg px-3 py-2 text-sm"
            >
              <option value="">All Sections</option>
              <option value="Cradle Roll">Cradle Roll</option>
              <option value="Junior Soldier">Junior Soldier</option>
              <option value="Youth (Young Women)">Youth (Young Women)</option>
              <option value="Youth (Young Men)">Youth (Young Men)</option>
              <option value="Home League">Home League</option>
              <option value="Men's Fellowship">Men's Fellowship</option>
              <option value="Senior Member (60+)">Senior Member (60+)</option>
              <option value="Senior Soldier">Senior Soldier</option>
            </select>
          </div>
          <div className="p-6">

          {loading ? (
            <div className="py-8">
              <LoadingSkeleton variant="table" count={5} />
            </div>
          ) : visibleRecords.length > 0 ? (
            <div className="overflow-x-auto -mx-2 px-2 sm:mx-0 sm:px-0">
              <table className="min-w-[860px] w-full">
                <thead>
                  <tr className="border-b border-slate-100">
                    <th className="px-4 py-3 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Record ID
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Created Date
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Participant
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Corps
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Age
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Verification
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-bold text-slate-500 uppercase tracking-wider">
                      Action
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {visibleRecords.map((record) => (
                    <tr key={record.id} className="hover:bg-slate-50/50 transition-colors duration-150 group">
                      <td className="px-4 py-4 whitespace-nowrap text-sm font-semibold text-brand-red">
                        {record.recordCode || 'N/A'}
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-600">
                        {new Date(record.createdAt).toLocaleDateString('en-US', { 
                          month: 'short', 
                          day: 'numeric',
                          year: 'numeric'
                        })}
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-3">
                          {record.personImageUrl ? (
                            <img src={record.personImageUrl} alt="participant" className="w-9 h-9 rounded-full object-cover border border-slate-200" />
                          ) : (
                            <div className="w-9 h-9 rounded-full bg-brand-navy/10 flex items-center justify-center">
                              <span className="text-brand-navy font-bold text-xs">
                                {record.firstName?.[0]}{record.familyName?.[0]}
                              </span>
                            </div>
                          )}
                          <span className="text-sm font-semibold text-slate-900">
                            {record.firstName} {record.familyName}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap text-sm text-slate-600 font-medium">
                        {getCorpsDisplayName(record.corpsName)}
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold ${
                          record.age < 16 
                            ? 'bg-rose-50 text-rose-600' 
                            : 'bg-purple-50 text-purple-600'
                        }`}>
                          {record.age < 16 ? <Baby size={12} /> : <UserCheck size={12} />}
                          {record.age} yrs
                        </span>
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap">
                        <Badge status={record.status} />
                      </td>
                      <td className="px-4 py-4 whitespace-nowrap text-right">
                        <Link
                          to={`/records/${record.id}`}
                          className="inline-flex items-center gap-1.5 text-blue-600 hover:text-blue-700 font-semibold text-sm transition-colors group/link"
                        >
                          {String(record.status || '').toUpperCase() === 'PENDING' ? (
                            <>
                              <CheckCircle size={16} />
                              Approve
                            </>
                          ) : (
                            <>
                              <Eye size={16} className="group-hover/link:scale-110 transition-transform" />
                              View
                            </>
                          )}
                        </Link>
                        {String(record.status || '').toUpperCase() === 'PENDING' && (
                          <>
                            <button onClick={() => updateRecordStatus(record.id, 'VERIFIED')} className="ml-3 text-emerald-700 font-semibold text-sm">Approve</button>
                            <button onClick={() => updateRecordStatus(record.id, 'DECLINED')} className="ml-2 text-red-700 font-semibold text-sm">Decline</button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-center py-16">
              <div className="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-gradient-to-br from-slate-50 to-slate-100 mb-4">
                <Search size={32} className="text-slate-300" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-2">No Records Yet</h3>
              <p className="text-slate-500 max-w-sm mx-auto mb-6">
                Select a category to view specific records, or add your first submission to get started.
              </p>
              <Link
                to="/records"
                className="inline-flex items-center gap-2 bg-brand-red hover:bg-brand-redDark text-white px-5 py-2.5 rounded-xl font-semibold shadow-lg shadow-red-900/20 transition-all"
              >
                <Plus size={18} />
                Add First Record
              </Link>
            </div>
          )}</div>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6">
          <h3 className="text-lg font-bold text-slate-900 mb-2">Verification Queue</h3>
          <p className="text-sm text-slate-500 mb-4">Pending approvals: <span className="font-semibold">{sectionStats.pending}</span></p>
          <div className="space-y-3">
            {pendingQueue.length === 0 && <p className="text-sm text-slate-500">No pending members.</p>}
            {pendingQueue.map((m) => (
              <div key={m.id} className="border border-slate-100 rounded-xl p-3 flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold text-slate-900">{m.firstName} {m.familyName}</p>
                  <p className="text-xs text-slate-500">{m.department || 'Unassigned'}</p>
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => updateRecordStatus(m.id, 'VERIFIED')} className="bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1.5 rounded-lg text-xs font-semibold">Approve</button>
                  <button onClick={() => updateRecordStatus(m.id, 'DECLINED')} className="bg-red-600 hover:bg-red-700 text-white px-3 py-1.5 rounded-lg text-xs font-semibold">Decline</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;

