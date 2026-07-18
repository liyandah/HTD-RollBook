import React, { useId, useRef } from 'react';
import { Camera, ImageIcon } from 'lucide-react';
import { prepareFileInput } from '../../utils/imagePicker';

const ImagePickButtons = ({ onFileSelected, disabled = false, compact = false }) => {
  const cameraInputId = useId();
  const galleryInputId = useId();
  const cameraInputRef = useRef(null);
  const galleryInputRef = useRef(null);

  const handleFileChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (file) {
      await onFileSelected(file);
    }
  };

  const openPicker = (mode) => {
    if (disabled) return;
    const input = mode === 'camera' ? cameraInputRef.current : galleryInputRef.current;
    prepareFileInput(input, mode);
  };

  const buttonClass = compact
    ? 'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors touch-manipulation'
    : 'flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors touch-manipulation';

  const enabledClass = 'bg-brand-red text-white hover:bg-brand-redDark cursor-pointer';
  const disabledClass = 'bg-gray-200 text-gray-500 cursor-not-allowed';

  return (
    <div className="flex flex-wrap items-center gap-2">
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

      <label
        htmlFor={cameraInputId}
        onClick={() => openPicker('camera')}
        className={`${buttonClass} ${disabled ? disabledClass : enabledClass}`}
      >
        <Camera className="w-4 h-4" />
        Take Photo
      </label>
      <label
        htmlFor={galleryInputId}
        onClick={() => openPicker('gallery')}
        className={`${buttonClass} ${disabled ? disabledClass : enabledClass}`}
      >
        <ImageIcon className="w-4 h-4" />
        Choose from Gallery
      </label>
    </div>
  );
};

export default ImagePickButtons;
