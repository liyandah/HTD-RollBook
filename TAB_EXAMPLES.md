# 📑 Horizontal Tabs - Implementation Examples

## Overview
This document provides complete examples of how to use the horizontal animated underline tabs throughout your application.

---

## 🎨 **Example 1: Records Page with Status Filters**

```jsx
import React, { useState } from 'react';
import Tabs from '../components/common/Tabs';
import { List, CheckCircle2, Clock, AlertTriangle } from 'lucide-react';

const RecordsPage = () => {
  const [activeFilter, setActiveFilter] = useState('all');
  const [records] = useState([/* your records data */]);

  // Calculate counts dynamically
  const counts = {
    all: records.length,
    completed: records.filter(r => r.status === 'COMPLETE').length,
    pending: records.filter(r => r.status === 'IN_PROGRESS').length,
    flagged: records.filter(r => r.flagged).length,
  };

  // Filter records based on active tab
  const filteredRecords = records.filter(record => {
    if (activeFilter === 'all') return true;
    if (activeFilter === 'completed') return record.status === 'COMPLETE';
    if (activeFilter === 'pending') return record.status === 'IN_PROGRESS';
    if (activeFilter === 'flagged') return record.flagged;
    return true;
  });

  return (
    <div className="space-y-0">
      <div className="bg-white p-6 border-b border-slate-200">
        <h1 className="text-2xl font-bold text-slate-900 mb-6">Data Records</h1>
        
        <Tabs
          tabs={[
            { id: 'all', label: 'All Records', icon: List, count: counts.all },
            { id: 'completed', label: 'Completed', icon: CheckCircle2, count: counts.completed },
            { id: 'pending', label: 'Pending', icon: Clock, count: counts.pending },
            { id: 'flagged', label: 'Flagged', icon: AlertTriangle, count: counts.flagged },
          ]}
          activeTab={activeFilter}
          onChange={setActiveFilter}
          variant="underline"
        />
      </div>

      {/* Your table/grid here */}
      <div className="p-6">
        {/* Display filteredRecords */}
      </div>
    </div>
  );
};
```

---

## 🎨 **Example 2: Dashboard with Time Period Filters**

```jsx
import React, { useState } from 'react';
import Tabs from '../components/common/Tabs';
import { Calendar, TrendingUp } from 'lucide-react';

const Dashboard = () => {
  const [timePeriod, setTimePeriod] = useState('week');

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-200">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-slate-900">Performance Overview</h2>
              <p className="text-sm text-slate-500 mt-1">Track your metrics over time</p>
            </div>
            
            <Tabs
              tabs={[
                { id: 'today', label: 'Today' },
                { id: 'week', label: 'This Week' },
                { id: 'month', label: 'This Month' },
                { id: 'year', label: 'This Year' },
              ]}
              activeTab={timePeriod}
              onChange={setTimePeriod}
              variant="underline"
            />
          </div>
        </div>
        
        <div className="p-6">
          {/* Your charts/stats based on timePeriod */}
        </div>
      </div>
    </div>
  );
};
```

---

## 🎨 **Example 3: User Management with Role Tabs**

```jsx
import React, { useState } from 'react';
import Tabs from '../components/common/Tabs';
import { Shield, Edit, Eye } from 'lucide-react';

const UserManagement = () => {
  const [roleFilter, setRoleFilter] = useState('all');
  const [users] = useState([/* your users data */]);

  const roleCounts = {
    all: users.length,
    admin: users.filter(u => u.role === 'Admin').length,
    editor: users.filter(u => u.role === 'Editor').length,
    viewer: users.filter(u => u.role === 'Viewer').length,
  };

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-slate-900">User Management</h1>
      
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <Tabs
          tabs={[
            { id: 'all', label: 'All Users', count: roleCounts.all },
            { id: 'admin', label: 'Administrators', icon: Shield, count: roleCounts.admin },
            { id: 'editor', label: 'Editors', icon: Edit, count: roleCounts.editor },
            { id: 'viewer', label: 'Viewers', icon: Eye, count: roleCounts.viewer },
          ]}
          activeTab={roleFilter}
          onChange={setRoleFilter}
          variant="underline"
        />
        
        {/* User table filtered by role */}
      </div>
    </div>
  );
};
```

---

## 🎨 **Example 4: Reports Page with Category Tabs**

```jsx
import React, { useState } from 'react';
import Tabs from '../components/common/Tabs';
import { BarChart3, PieChart, TrendingUp, Activity } from 'lucide-react';

const ReportsPage = () => {
  const [reportType, setReportType] = useState('overview');

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Analytics & Reports</h1>
          <p className="text-slate-500 mt-1">View detailed insights</p>
        </div>
      </div>
      
      <Tabs
        tabs={[
          { id: 'overview', label: 'Overview', icon: BarChart3 },
          { id: 'distribution', label: 'Distribution', icon: PieChart },
          { id: 'trends', label: 'Trends', icon: TrendingUp },
          { id: 'activity', label: 'Activity', icon: Activity },
        ]}
        activeTab={reportType}
        onChange={setReportType}
        variant="underline"
      />
      
      {/* Different report views based on reportType */}
      {reportType === 'overview' && <OverviewCharts />}
      {reportType === 'distribution' && <DistributionCharts />}
      {reportType === 'trends' && <TrendsCharts />}
      {reportType === 'activity' && <ActivityLog />}
    </div>
  );
};
```

---

## 🎨 **Example 5: Settings Page with Section Tabs**

```jsx
import React, { useState } from 'react';
import Tabs from '../components/common/Tabs';
import { User, Bell, Shield, Globe } from 'lucide-react';

const SettingsPage = () => {
  const [section, setSection] = useState('profile');

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-slate-900">Settings</h1>
      
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <Tabs
          tabs={[
            { id: 'profile', label: 'Profile', icon: User },
            { id: 'notifications', label: 'Notifications', icon: Bell },
            { id: 'security', label: 'Security', icon: Shield },
            { id: 'preferences', label: 'Preferences', icon: Globe },
          ]}
          activeTab={section}
          onChange={setSection}
          variant="underline"
        />
        
        <div className="p-8">
          {section === 'profile' && <ProfileSettings />}
          {section === 'notifications' && <NotificationSettings />}
          {section === 'security' && <SecuritySettings />}
          {section === 'preferences' && <PreferenceSettings />}
        </div>
      </div>
    </div>
  );
};
```

---

## 🎨 **Design Patterns**

### **Pattern 1: Simple Text Tabs**
Best for: Clean interfaces with few options
```jsx
<Tabs
  tabs={[
    { id: 'tab1', label: 'Option 1' },
    { id: 'tab2', label: 'Option 2' },
  ]}
  activeTab={active}
  onChange={setActive}
  variant="underline"
/>
```

### **Pattern 2: Icon + Text Tabs**
Best for: Visual clarity and faster recognition
```jsx
<Tabs
  tabs={[
    { id: 'tab1', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'tab2', label: 'Reports', icon: BarChart3 },
  ]}
  activeTab={active}
  onChange={setActive}
  variant="underline"
/>
```

### **Pattern 3: Tabs with Count Badges**
Best for: Action-required interfaces
```jsx
<Tabs
  tabs={[
    { id: 'all', label: 'All', count: 124 },
    { id: 'pending', label: 'Pending', count: 12 },
  ]}
  activeTab={active}
  onChange={setActive}
  variant="underline"
/>
```

### **Pattern 4: Full Featured Tabs**
Best for: Complex filtering interfaces
```jsx
<Tabs
  tabs={[
    { id: 'all', label: 'All Records', icon: List, count: 124 },
    { id: 'flagged', label: 'Flagged', icon: AlertTriangle, count: 3 },
  ]}
  activeTab={active}
  onChange={setActive}
  variant="underline"
/>
```

---

## 🎯 **Best Practices**

### **1. Use Meaningful Counts**
```jsx
// ✅ Good - Dynamic counts
count: records.filter(r => r.status === 'PENDING').length

// ❌ Bad - Hardcoded counts
count: 5
```

### **2. Filter Data Based on Active Tab**
```jsx
const filteredData = data.filter(item => {
  if (activeTab === 'all') return true;
  return item.category === activeTab;
});
```

### **3. Organize Tabs Logically**
```jsx
// ✅ Good - Most common first
['All', 'Active', 'Completed', 'Archived']

// ❌ Bad - Random order
['Archived', 'Active', 'All', 'Completed']
```

### **4. Use Icons Consistently**
```jsx
// ✅ Good - Icons for all tabs or none
tabs={[
  { id: 'all', label: 'All', icon: List },
  { id: 'active', label: 'Active', icon: Activity },
]}

// ❌ Bad - Some have icons, some don't
tabs={[
  { id: 'all', label: 'All', icon: List },
  { id: 'active', label: 'Active' },
]}
```

---

## 🎨 **Styling Customization**

### **Change Colors**
Edit your `tailwind.config.js`:
```js
colors: {
  brand: {
    red: '#C61F2C',  // Active tab color
  }
}
```

### **Adjust Spacing**
In the Tabs component:
```jsx
// Change gap between tabs
<nav className="flex gap-8">  // Default is gap-8

// Change padding
<button className="py-4">  // Vertical padding
```

### **Modify Animation**
```jsx
// Current: fadeIn animation
className="animate-fadeIn"

// Add custom animations in tailwind.config.js
animation: {
  'slide-in': 'slideIn 0.3s ease-out',
}
```

---

## 📱 **Responsive Behavior**

### **Desktop (lg+)**
```jsx
<nav className="flex gap-8">
  {/* All tabs visible */}
</nav>
```

### **Tablet (md)**
```jsx
<nav className="flex gap-6 overflow-x-auto">
  {/* Scrollable if needed */}
</nav>
```

### **Mobile (sm)**
```jsx
<nav className="flex gap-4 overflow-x-auto pb-2">
  {/* Compact spacing, scrollable */}
</nav>
```

---

## ✨ **Animation Details**

### **Underline Animation**
```jsx
{isActive && (
  <span className="
    absolute bottom-0 left-0 right-0 
    h-0.5 bg-brand-red rounded-t-full 
    animate-fadeIn
  " />
)}
```

### **Badge Animation**
```jsx
className={`
  ${isActive ? 'scale-110' : ''}  // Slightly larger when active
  transition-all duration-200      // Smooth transition
`}
```

### **Icon Animation**
```jsx
className={`
  ${isActive ? 'scale-110' : 'group-hover:scale-110'}
  transition-transform duration-200
`}
```

---

## 🚀 **Quick Start**

### **1. Import the Component**
```jsx
import Tabs from '../components/common/Tabs';
```

### **2. Add State**
```jsx
const [activeTab, setActiveTab] = useState('all');
```

### **3. Render Tabs**
```jsx
<Tabs
  tabs={[
    { id: 'all', label: 'All' },
    { id: 'active', label: 'Active' },
  ]}
  activeTab={activeTab}
  onChange={setActiveTab}
  variant="underline"
/>
```

### **4. Use Active State**
```jsx
{activeTab === 'all' && <AllContent />}
{activeTab === 'active' && <ActiveContent />}
```

---

## 🎉 **Result**

You'll have professional, animated tabs that:
- ✅ Provide instant visual feedback
- ✅ Show action counts with badges
- ✅ Have smooth hover and active states
- ✅ Work perfectly on all devices
- ✅ Match your brand colors
- ✅ Feel responsive and modern

---

**Now go add these beautiful tabs to all your pages! 🚀**






