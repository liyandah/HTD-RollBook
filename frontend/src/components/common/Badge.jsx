import React from 'react';

const Badge = ({ status }) => {
  const getStatusStyles = () => {
    switch (status) {
      case 'COMPLETE':
        return 'bg-green-100 text-green-800';
      case 'IN_PROGRESS':
        return 'bg-yellow-100 text-yellow-800';
      case 'PENDING':
        return 'bg-amber-100 text-amber-800';
      case 'VERIFIED':
        return 'bg-blue-100 text-blue-800';
      case 'DECLINED':
        return 'bg-red-100 text-red-700';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusLabel = () => {
    switch (status) {
      case 'COMPLETE':
        return 'Complete';
      case 'IN_PROGRESS':
        return 'In Progress';
      case 'PENDING':
        return 'Pending';
      case 'VERIFIED':
        return 'Verified';
      case 'DECLINED':
        return 'Declined';
      default:
        return status;
    }
  };

  return (
    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusStyles()}`}>
      {getStatusLabel()}
    </span>
  );
};

export default Badge;






