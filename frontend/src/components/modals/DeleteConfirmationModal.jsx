import React from 'react';
import Modal from '../common/Modal';
import { AlertCircle, Trash2 } from 'lucide-react';

const DeleteConfirmationModal = ({ isOpen, onClose, onConfirm, itemName }) => {
  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Confirm Deletion" maxWidth="max-w-md">
      <div className="p-6 text-center space-y-4">
        <AlertCircle size={48} className="text-red-500 mx-auto" />
        <h3 className="text-xl font-semibold text-slate-900">Are you sure?</h3>
        <p className="text-slate-600">
          You are about to delete the {itemName || 'item'}. This action cannot be undone.
        </p>
        <div className="flex justify-center gap-4 mt-6">
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-lg text-slate-700 border border-slate-300 hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className="px-5 py-2.5 rounded-lg bg-red-600 text-white hover:bg-red-700 transition-colors flex items-center gap-2"
          >
            <Trash2 size={18} />
            Delete
          </button>
        </div>
      </div>
    </Modal>
  );
};

export default DeleteConfirmationModal;
