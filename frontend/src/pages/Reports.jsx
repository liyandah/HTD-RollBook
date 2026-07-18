import React, { useState, useEffect } from 'react';
import { 
  BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { 
  TrendingUp, Users, CheckCircle, Clock, Download, 
  Calendar, Baby, UserCheck, BarChart3 
} from 'lucide-react';
import http from '../api/apiClient';
import LoadingSkeleton from '../components/common/LoadingSkeleton';

const Reports = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(null);

  useEffect(() => {
    fetchReportData();
  }, []);

  const [monthlyData, setMonthlyData] = useState([]);

  const fetchReportData = async () => {
    try {
      setLoading(true);
      const [dashboardResponse, monthlyResponse] = await Promise.all([
        http.get('/api/records/dashboard'),
        http.get('/api/reports/monthly?months=6')
      ]);
      
      setStats(dashboardResponse.data);
      
      // Transform monthly data for charts
      const transformedData = monthlyResponse.data.monthlyData.map(item => ({
        month: item.month,
        records: item.totalRecords,
        verified: item.verifiedRecords,
        completed: item.completedRecords
      }));
      setMonthlyData(transformedData);
    } catch (err) {
      console.error('Failed to load report data:', err);
      // Fallback to empty data
      setMonthlyData([]);
    } finally {
      setLoading(false);
    }
  };

  const statusDistribution = [
    { name: 'Verified', value: stats?.verifiedRecords || 0, color: '#10b981' },
    { name: 'In Progress', value: stats?.inProgressRecords || 0, color: '#f59e0b' },
    { name: 'Complete', value: stats?.completedRecords || 0, color: '#3b82f6' },
  ];

  const ageDistribution = [
    { name: 'Under 16', value: stats?.under16Count || 0, color: '#f43f5e' },
    { name: 'Age 16+', value: stats?.age16AndAboveCount || 0, color: '#a855f7' },
  ];

  if (loading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-slate-900">Reports & Analytics</h1>
        <LoadingSkeleton count={3} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Reports & Analytics</h1>
          <p className="text-slate-500 mt-1">Data insights and trends overview</p>
        </div>
        <button className="inline-flex items-center gap-2 bg-brand-red hover:bg-brand-redDark text-white px-4 py-2.5 rounded-xl font-semibold transition-all shadow-lg shadow-red-900/20">
          <Download size={18} />
          Export Report
        </button>
      </div>

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 text-white p-6 rounded-2xl shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <Users size={24} className="opacity-80" />
            <TrendingUp size={20} className="opacity-60" />
          </div>
          <p className="text-blue-100 text-sm font-medium">Total Records</p>
          <p className="text-4xl font-bold mt-2">{stats?.totalRecords || 0}</p>
          <p className="text-blue-200 text-xs mt-3">All time entries</p>
        </div>

        <div className="bg-gradient-to-br from-emerald-500 to-emerald-600 text-white p-6 rounded-2xl shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <CheckCircle size={24} className="opacity-80" />
            <BarChart3 size={20} className="opacity-60" />
          </div>
          <p className="text-emerald-100 text-sm font-medium">Verified</p>
          <p className="text-4xl font-bold mt-2">{stats?.verifiedRecords || 0}</p>
          <p className="text-emerald-200 text-xs mt-3">
            {stats?.totalRecords > 0 
              ? `${((stats.verifiedRecords / stats.totalRecords) * 100).toFixed(1)}% of total`
              : '0% of total'}
          </p>
        </div>

        <div className="bg-gradient-to-br from-amber-500 to-amber-600 text-white p-6 rounded-2xl shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <Clock size={24} className="opacity-80" />
            <Calendar size={20} className="opacity-60" />
          </div>
          <p className="text-amber-100 text-sm font-medium">In Progress</p>
          <p className="text-4xl font-bold mt-2">{stats?.inProgressRecords || 0}</p>
          <p className="text-amber-200 text-xs mt-3">Pending verification</p>
        </div>

        <div className="bg-gradient-to-br from-purple-500 to-purple-600 text-white p-6 rounded-2xl shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <Baby size={24} className="opacity-80" />
            <UserCheck size={20} className="opacity-60" />
          </div>
          <p className="text-purple-100 text-sm font-medium">Under 16</p>
          <p className="text-4xl font-bold mt-2">{stats?.under16Count || 0}</p>
          <p className="text-purple-200 text-xs mt-3">Youth records</p>
        </div>
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Monthly Trends Chart */}
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Registration Trends</h3>
              <p className="text-sm text-slate-500 mt-1">Monthly data collection overview</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="flex items-center gap-1.5 text-xs font-medium text-slate-600">
                <span className="w-3 h-3 rounded-full bg-brand-red"></span>
                Total
              </span>
              <span className="flex items-center gap-1.5 text-xs font-medium text-slate-600">
                <span className="w-3 h-3 rounded-full bg-emerald-500"></span>
                Verified
              </span>
            </div>
          </div>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthlyData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis 
                  dataKey="month" 
                  axisLine={false} 
                  tickLine={false}
                  style={{ fontSize: '12px', fill: '#64748b' }}
                />
                <YAxis 
                  axisLine={false} 
                  tickLine={false}
                  style={{ fontSize: '12px', fill: '#64748b' }}
                />
                <Tooltip 
                  cursor={{ fill: '#f8fafc' }}
                  contentStyle={{
                    borderRadius: '12px',
                    border: 'none',
                    boxShadow: '0 10px 15px -3px rgba(0,0,0,0.1)',
                    padding: '12px'
                  }}
                />
                <Bar dataKey="records" fill="#C61F2C" radius={[8, 8, 0, 0]} />
                <Bar dataKey="verified" fill="#10b981" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Impact Summary Card */}
        <div className="bg-gradient-to-br from-brand-navy to-brand-navyDark text-white p-8 rounded-2xl shadow-xl flex flex-col justify-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-white/10 mb-6">
            <TrendingUp size={28} className="text-emerald-400" />
          </div>
          <h3 className="text-blue-200 text-sm font-semibold uppercase tracking-wider">Total Impact</h3>
          <p className="text-6xl font-bold mt-3">{stats?.totalRecords || 0}</p>
          <div className="mt-6 pt-6 border-t border-white/10">
            <p className="text-blue-100 text-sm leading-relaxed">
              Data collection has{' '}
              <span className="inline-flex items-center gap-1 text-emerald-400 font-bold">
                <TrendingUp size={16} />
                increased by 12%
              </span>
              {' '}compared to last month
            </p>
          </div>
          <div className="mt-6 grid grid-cols-2 gap-4">
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-blue-300 text-xs font-medium">Completion Rate</p>
              <p className="text-2xl font-bold mt-1">
                {stats?.totalRecords > 0
                  ? `${((stats.completedRecords / stats.totalRecords) * 100).toFixed(0)}%`
                  : '0%'}
              </p>
            </div>
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-blue-300 text-xs font-medium">Verification</p>
              <p className="text-2xl font-bold mt-1">
                {stats?.totalRecords > 0
                  ? `${((stats.verifiedRecords / stats.totalRecords) * 100).toFixed(0)}%`
                  : '0%'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Charts Row 2 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Status Distribution */}
        <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
          <h3 className="text-lg font-bold text-slate-900 mb-2">Status Distribution</h3>
          <p className="text-sm text-slate-500 mb-6">Breakdown by verification status</p>
          <div className="h-72 flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={statusDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {statusDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="grid grid-cols-3 gap-3 mt-4">
            {statusDistribution.map((item, index) => (
              <div key={index} className="text-center p-3 bg-slate-50 rounded-xl">
                <div className="w-4 h-4 rounded-full mx-auto mb-2" style={{ backgroundColor: item.color }}></div>
                <p className="text-xs font-medium text-slate-600">{item.name}</p>
                <p className="text-lg font-bold text-slate-900 mt-1">{item.value}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Age Distribution */}
        <div className="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm">
          <h3 className="text-lg font-bold text-slate-900 mb-2">Age Distribution</h3>
          <p className="text-sm text-slate-500 mb-6">Participants by age category</p>
          <div className="h-72 flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={ageDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {ageDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="grid grid-cols-2 gap-3 mt-4">
            {ageDistribution.map((item, index) => (
              <div key={index} className="text-center p-3 bg-slate-50 rounded-xl">
                <div className="w-4 h-4 rounded-full mx-auto mb-2" style={{ backgroundColor: item.color }}></div>
                <p className="text-xs font-medium text-slate-600">{item.name}</p>
                <p className="text-lg font-bold text-slate-900 mt-1">{item.value}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Key Insights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-emerald-50 border-2 border-emerald-200 p-6 rounded-2xl">
          <div className="w-12 h-12 bg-emerald-600 rounded-xl flex items-center justify-center mb-4">
            <CheckCircle size={24} className="text-white" />
          </div>
          <h4 className="text-sm font-semibold text-emerald-900 uppercase tracking-wider mb-2">Fastest Verification</h4>
          <p className="text-2xl font-bold text-emerald-900">2.3 Days</p>
          <p className="text-sm text-emerald-700 mt-2">Average processing time</p>
        </div>

        <div className="bg-purple-50 border-2 border-purple-200 p-6 rounded-2xl">
          <div className="w-12 h-12 bg-purple-600 rounded-xl flex items-center justify-center mb-4">
            <TrendingUp size={24} className="text-white" />
          </div>
          <h4 className="text-sm font-semibold text-purple-900 uppercase tracking-wider mb-2">Growth Rate</h4>
          <p className="text-2xl font-bold text-purple-900">+18.5%</p>
          <p className="text-sm text-purple-700 mt-2">Compared to last quarter</p>
        </div>
      </div>
    </div>
  );
};

export default Reports;


