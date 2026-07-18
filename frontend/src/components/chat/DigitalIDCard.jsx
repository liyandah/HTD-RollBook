import React, { forwardRef } from 'react';
import { CheckCircle2 } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

const DigitalIDCard = forwardRef(function DigitalIDCard(
  { memberName, memberId, corpsName = 'Highfield Temple', memberType, photoUrl, enrolledAt },
  ref
) {
  return (
    <div ref={ref} className="rounded-2xl border-2 border-[#C21124] bg-white overflow-hidden shadow-md">
      <div className="bg-[#C21124] text-white px-4 py-3">
        <div className="flex items-center gap-2">
          <img src="/shield.jpg?v=20260414" alt="Salvation Army Shield" className="w-7 h-7 rounded-full bg-white p-0.5" />
          <p className="font-bold text-xs tracking-wide">OFFICIAL MEMBER CARD</p>
        </div>
      </div>

      <div className="p-4 grid grid-cols-[72px,1fr,88px] gap-3 items-center">
        <div className="w-[72px] h-[72px] rounded-full border border-gray-200 overflow-hidden bg-gray-100">
          {photoUrl ? (
            <img src={photoUrl} alt="Member profile" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-xs text-gray-500">No Photo</div>
          )}
        </div>

        <div className="min-w-0">
          <p className="font-bold text-gray-900 truncate">{memberName || 'Member Name'}</p>
          <p className="text-xs text-gray-600 truncate">Corps: {corpsName}</p>
          <p className="text-xs text-gray-600 truncate">Member Type: {memberType || 'Registered Member'}</p>
          <p className="text-xs font-mono text-[#7b1020] truncate">ID: {memberId || 'Pending ID'}</p>
        </div>

        <div className="justify-self-end">
          <QRCodeSVG value={memberId || 'PENDING_MEMBER_ID'} size={82} />
        </div>
      </div>

      <div className="px-4 pb-3 flex items-center justify-between text-xs text-gray-600">
        <div className="inline-flex items-center gap-1 text-green-700 font-semibold">
          <CheckCircle2 className="w-3.5 h-3.5" />
          Verified
        </div>
        <p>Enrolled: {enrolledAt || new Date().toLocaleDateString()}</p>
      </div>
    </div>
  );
});

export default DigitalIDCard;
