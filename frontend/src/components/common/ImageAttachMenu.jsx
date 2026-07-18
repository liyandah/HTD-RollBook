import React, { useEffect, useId, useRef, useState } from 'react';
import { Camera, ImageIcon, Paperclip, X } from 'lucide-react';
import { prepareFileInput } from '../../utils/imagePicker';

const ImageAttachMenu = ({
  onFileSelected,
  disabled = false,
  className = '',
  iconClassName = 'w-5 h-5',
  title = 'Attach image',
}) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const cameraInputId = useId();
  const galleryInputId = useId();
  const cameraInputRef = useRef(null);
  const galleryInputRef = useRef(null);

  useEffect(() => {
    if (!menuOpen) return undefined;

    const handleOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutside);
    document.addEventListener('touchstart', handleOutside);
    return () => {
      document.removeEventListener('mousedown', handleOutside);
      document.removeEventListener('touchstart', handleOutside);
    };
  }, [menuOpen]);

  const handleFileChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    setMenuOpen(false);
    if (file) {
      await onFileSelected(file);
    }
  };

  const openPicker = (mode) => {
    if (disabled) return;
    const input = mode === 'camera' ? cameraInputRef.current : galleryInputRef.current;
    prepareFileInput(input, mode);
    setMenuOpen(false);
  };

  const handleAttachClick = (event) => {
    event.preventDefault();
    event.stopPropagation();
    if (disabled) return;
    setMenuOpen((open) => !open);
  };

  return (
    <div ref={menuRef} className={`relative flex-shrink-0 ${className}`}>
      <input
        ref={cameraInputRef}
        id={cameraInputId}
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleFileChange}
        className="sr-only"
        tabIndex={-1}
        aria-hidden="true"
        disabled={disabled}
      />
      <input
        ref={galleryInputRef}
        id={galleryInputId}
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        className="sr-only"
        tabIndex={-1}
        aria-hidden="true"
        disabled={disabled}
      />

      <button
        type="button"
        onClick={handleAttachClick}
        disabled={disabled}
        aria-label={title}
        title={title}
        className={`min-w-[44px] min-h-[44px] p-2 sm:p-2.5 text-gray-500 hover:text-gray-700 transition-colors touch-manipulation flex items-center justify-center ${
          disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
        }`}
      >
        <Paperclip className={iconClassName} />
      </button>

      {menuOpen && (
        <div className="absolute bottom-full left-0 mb-2 w-52 rounded-xl border border-gray-200 bg-white shadow-lg z-50 overflow-hidden">
          <div className="flex items-center justify-between px-3 py-2 border-b border-gray-100">
            <span className="text-xs font-semibold text-gray-700">Add photo</span>
            <button
              type="button"
              onClick={() => setMenuOpen(false)}
              className="p-1 text-gray-400 hover:text-gray-600"
              aria-label="Close"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
          <label
            htmlFor={cameraInputId}
            onClick={() => openPicker('camera')}
            className="w-full flex items-center gap-3 px-4 py-3 text-sm text-gray-800 hover:bg-gray-50 touch-manipulation cursor-pointer"
          >
            <Camera className="w-4 h-4 text-brand-red" />
            Take Photo
          </label>
          <label
            htmlFor={galleryInputId}
            onClick={() => openPicker('gallery')}
            className="w-full flex items-center gap-3 px-4 py-3 text-sm text-gray-800 hover:bg-gray-50 touch-manipulation cursor-pointer border-t border-gray-100"
          >
            <ImageIcon className="w-4 h-4 text-brand-red" />
            Choose from Gallery
          </label>
        </div>
      )}
    </div>
  );
};

export default ImageAttachMenu;
