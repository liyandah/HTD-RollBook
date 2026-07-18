import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

const SidebarItem = ({ item, userRole, onLinkClick }) => {
  const location = useLocation();
  const [subMenuOpen, setSubMenuOpen] = useState(false);
  
  const isActive = (path) => {
    return location.pathname.startsWith(path);
  };

  const active = isActive(item.path);
  const hasSubItems = item.subItems && item.subItems.length > 0;

  // Filter sub-items by role
  const visibleSubItems = hasSubItems 
    ? item.subItems.filter(subItem => {
        if (!subItem.roles) return true;
        return subItem.roles.includes(userRole);
      })
    : [];

  // Hide main item if no visible sub-items
  if (hasSubItems && visibleSubItems.length === 0) {
    return null;
  }

  const Icon = item.icon;

  if (hasSubItems) {
    return (
      <div>
        <button
          onClick={() => setSubMenuOpen(!subMenuOpen)}
          className={`
            w-full relative flex items-center justify-between gap-3 px-4 py-3 rounded-xl font-medium transition-all duration-200 group
            ${active 
              ? 'bg-white text-brand-navy shadow-lg' 
              : 'text-blue-100 hover:bg-white/10 hover:text-white'}
          `}
        >
          <div className="flex items-center gap-3">
            <Icon 
              size={20} 
              className={`transition-transform duration-200 ${
                active 
                  ? 'scale-110 text-brand-red' 
                  : 'text-blue-300 group-hover:scale-110 group-hover:text-white'
              }`} 
              strokeWidth={2.5}
            />
            <span className={`text-sm font-semibold tracking-wide ${
              active ? 'text-brand-navy' : ''
            }`}>
              {item.label}
            </span>
          </div>
          <span className={`text-xs transition-transform ${subMenuOpen ? 'rotate-180' : ''}`}>▼</span>
        </button>
        {subMenuOpen && (
          <div className="ml-4 mt-2 space-y-1">
            {visibleSubItems.map((subItem) => {
              const subActive = isActive(subItem.path);
              return (
                <Link
                  key={subItem.path}
                  to={subItem.path}
                  onClick={onLinkClick}
                  className={`
                    block px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200
                    ${subActive 
                      ? 'bg-white/20 text-white' 
                      : 'text-blue-200 hover:bg-white/10 hover:text-white'}
                  `}
                >
                  {subItem.label}
                </Link>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  return (
    <Link
      to={item.path}
      onClick={onLinkClick}
      className={`
        relative flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all duration-200 group
        ${active 
          ? 'bg-white text-brand-navy shadow-lg' 
          : 'text-blue-100 hover:bg-white/10 hover:text-white'}
      `}
    >
      {/* Active Indicator */}
      {active && (
        <span className="absolute left-0 w-1 h-8 bg-brand-red rounded-r-full" />
      )}
      
      <Icon 
        size={20} 
        className={`transition-transform duration-200 ${
          active 
            ? 'scale-110 text-brand-red' 
            : 'text-blue-300 group-hover:scale-110 group-hover:text-white'
        }`} 
        strokeWidth={2.5}
      />
      
      <span className={`text-sm font-semibold tracking-wide ${
        active ? 'text-brand-navy' : ''
      }`}>
        {item.label}
      </span>

      {/* Hover Glow Effect */}
      {!active && (
        <div className="absolute inset-0 rounded-xl bg-white opacity-0 group-hover:opacity-5 transition-opacity duration-200" />
      )}
    </Link>
  );
};

export default SidebarItem;
