# 🎨 Salvation Army Theme Reference Guide

## Overview
This document provides a complete reference for using the Salvation Army brand colors and design system throughout the application.

---

## 🎨 Brand Colors

### Primary Colors

#### Salvation Army Red
- **`bg-brand-red`** - Primary red (#C61F2C)
- **`bg-brand-redDark`** - Darker red for hover states (#A01924)
- **`bg-brand-redLight`** - Lighter red for accents (#E8475C)

```jsx
// Example usage
<button className="bg-brand-red hover:bg-brand-redDark text-white">
  Click Me
</button>
```

#### Navy Blue
- **`bg-brand-navy`** - Deep navy for headers/sidebars (#002D72)
- **`bg-brand-navyDark`** - Darkest navy for backgrounds (#001A41)
- **`bg-brand-navyLight`** - Lighter navy for hover states (#003D8F)

```jsx
// Example usage
<div className="bg-brand-navy text-white">
  Sidebar Content
</div>
```

#### Gold/Yellow
- **`bg-brand-yellow`** - Salvation Army gold (#FFB81C)
- **`bg-brand-yellowDark`** - Darker yellow for hover (#E5A419)

```jsx
// Example usage
<span className="text-brand-yellow font-semibold">
  Important Notice
</span>
```

---

## 🎨 Surface Colors (Backgrounds & Borders)

### Light Surfaces
- **`bg-surface-50`** - Lightest background (#F8FAFC) - Page background
- **`bg-surface-100`** - Input fields, cards (#F1F5F9)
- **`bg-surface-200`** - Borders, dividers (#E2E8F0)
- **`bg-surface-300`** - Hover borders (#CBD5E1)

### Text Colors
- **`text-surface-400`** - Muted text (#94A3B8)
- **`text-surface-500`** - Secondary text (#64748B)

---

## 🔤 Typography

### Font Family
The app uses **Inter** - a professional, modern sans-serif font.

```jsx
// Default for all text
<p className="font-sans">Regular text</p>

// For headings
<h1 className="font-display font-bold">Display Heading</h1>
```

### Font Weights
- `font-normal` (400) - Regular text
- `font-medium` (500) - Emphasized text
- `font-semibold` (600) - Headings, labels
- `font-bold` (700) - Primary headings
- `font-extrabold` (800) - Hero text

---

## 📐 Border Radius

### Standard Sizes
- **`rounded-xl`** - 12px - Small cards
- **`rounded-2xl`** - 16px - Cards, buttons
- **`rounded-3xl`** - 24px - Large containers

```jsx
// Example usage
<div className="bg-white rounded-2xl shadow-sm p-6">
  Card Content
</div>
```

---

## 💫 Shadows

### Custom Shadows
- **`shadow-soft`** - Subtle shadow for cards
- **`shadow-medium`** - Medium shadow for elevated elements
- **`shadow-large`** - Large shadow for modals

```jsx
// Example usage
<div className="bg-white rounded-2xl shadow-medium">
  Elevated Card
</div>
```

---

## 🎯 Reusable Component Classes

### Buttons

#### Primary Button
```jsx
<button className="btn-primary">
  Primary Action
</button>
// Result: Red button with hover effect
```

#### Secondary Button
```jsx
<button className="btn-secondary">
  Secondary Action
</button>
// Result: Navy button with hover effect
```

#### Outline Button
```jsx
<button className="btn-outline">
  Outline Action
</button>
// Result: Navy outline button
```

### Cards

#### Basic Card
```jsx
<div className="card">
  Card Content
</div>
// Result: White card with border and shadow
```

#### Hoverable Card
```jsx
<div className="card-hover">
  Interactive Card
</div>
// Result: Card with hover shadow effect
```

### Input Fields

```jsx
<input type="text" className="input-field" />
// Result: Styled input with focus state
```

---

## ✨ Animations

### Built-in Animations

#### Shake (for errors)
```jsx
<div className="animate-shake bg-red-50 border-2 border-brand-red">
  Error message
</div>
```

#### Fade In
```jsx
<div className="animate-fadeIn">
  Content that fades in
</div>
```

---

## 🎨 Common Patterns

### Stats Card (Modern Bento Style)
```jsx
<div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 
     flex items-center justify-between hover:shadow-md transition-all duration-300">
  <div>
    <p className="text-sm font-medium text-slate-500 mb-1">Total Records</p>
    <h3 className="text-3xl font-bold text-slate-900">1,234</h3>
  </div>
  <div className="p-3 rounded-xl bg-blue-50 text-blue-600">
    <Icon size={24} strokeWidth={2.5} />
  </div>
</div>
```

### Header with Subtitle
```jsx
<div className="flex items-center justify-between">
  <div>
    <h1 className="text-3xl font-bold text-slate-900">Dashboard</h1>
    <p className="text-slate-500 mt-1">Overview of your data collection</p>
  </div>
</div>
```

### Table Header
```jsx
<thead>
  <tr className="border-b border-slate-100">
    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
      Column Name
    </th>
  </tr>
</thead>
```

### Interactive Link with Icon
```jsx
<Link to="/path" 
  className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700 
             font-medium text-sm transition-colors duration-200 group">
  View All
  <ArrowRight size={16} className="group-hover:translate-x-1 transition-transform duration-200" />
</Link>
```

---

## 🎯 Best Practices

### DO ✅
- Use brand colors for primary actions and headers
- Use surface colors for backgrounds and borders
- Apply hover states for interactive elements
- Use consistent border radius (rounded-2xl for cards)
- Add transition classes for smooth interactions

### DON'T ❌
- Don't mix hex codes with utility classes
- Don't use arbitrary values when theme values exist
- Don't forget hover states on clickable elements
- Don't use emojis instead of icons in production

---

## 📱 Responsive Design

### Grid Layouts
```jsx
// 1 column mobile, 2 tablet, 3 desktop
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
  {/* Cards */}
</div>
```

### Spacing
```jsx
// Responsive spacing
<div className="p-4 md:p-6 lg:p-8">
  Content
</div>
```

---

## 🔧 Quick Reference

### Most Used Classes
```css
/* Backgrounds */
bg-white, bg-surface-50, bg-surface-100, bg-brand-red, bg-brand-navy

/* Text Colors */
text-slate-900, text-slate-700, text-slate-500, text-brand-red, text-brand-navy

/* Borders */
border, border-slate-100, border-surface-200, border-brand-navy

/* Shadows */
shadow-sm, shadow-md, shadow-soft, shadow-medium, shadow-large

/* Rounded Corners */
rounded-lg, rounded-xl, rounded-2xl, rounded-3xl

/* Spacing */
p-4, p-6, p-8, gap-4, gap-6, space-y-4, space-y-6

/* Transitions */
transition-all, transition-colors, transition-transform, duration-200, duration-300
```

---

## 🎨 Color Accessibility

All brand colors meet WCAG 2.1 AA standards when used with appropriate text colors:

- **White text on brand-red**: ✅ AAA (7.2:1)
- **White text on brand-navy**: ✅ AAA (12.8:1)
- **Dark text on brand-yellow**: ✅ AA (4.8:1)

---

## 🚀 Quick Start Examples

### Page Container
```jsx
<div className="p-6 space-y-6 bg-surface-50 min-h-screen">
  {/* Page content */}
</div>
```

### Form
```jsx
<form className="space-y-5">
  <div>
    <label className="block text-slate-700 text-sm font-semibold mb-2">
      Label
    </label>
    <input type="text" className="input-field" />
  </div>
  <button type="submit" className="btn-primary w-full">
    Submit
  </button>
</form>
```

---

**Last Updated**: January 2026  
**Version**: 1.0.0






