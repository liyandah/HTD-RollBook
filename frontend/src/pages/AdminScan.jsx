import React, { useEffect, useRef, useState } from 'react';
import http from '../api/apiClient';

const AdminScan = () => {
  const [code, setCode] = useState('');
  const [message, setMessage] = useState('');
  const [isScanning, setIsScanning] = useState(false);
  const [supportsBarcodeDetector, setSupportsBarcodeDetector] = useState(false);
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const rafRef = useRef(null);

  const markAttendance = async (memberId) => {
    if (!memberId) return;
    try {
      await http.post('/api/attendance/mark', {
        memberId,
        timestamp: new Date().toISOString(),
      });
      setMessage(`Attendance marked for ${memberId}.`);
      setCode('');
    } catch (err) {
      setMessage(
        err?.response?.data?.message ||
          'Could not mark attendance. Ensure /api/attendance/mark exists on backend.'
      );
    }
  };

  const stopScan = () => {
    setIsScanning(false);
    if (rafRef.current) cancelAnimationFrame(rafRef.current);
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
  };

  useEffect(() => {
    setSupportsBarcodeDetector(typeof window !== 'undefined' && 'BarcodeDetector' in window);
    return () => stopScan();
  }, []);

  const startScan = async () => {
    if (!supportsBarcodeDetector || isScanning) return;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      const detector = new window.BarcodeDetector({
        formats: ['qr_code'],
      });
      setIsScanning(true);

      const tick = async () => {
        if (!videoRef.current || !isScanning) return;
        try {
          const barcodes = await detector.detect(videoRef.current);
          if (barcodes.length > 0 && barcodes[0].rawValue) {
            const scanned = barcodes[0].rawValue.trim();
            setCode(scanned);
            stopScan();
            await markAttendance(scanned);
            return;
          }
        } catch (e) {
          // Continue scanning loop.
        }
        rafRef.current = requestAnimationFrame(tick);
      };
      rafRef.current = requestAnimationFrame(tick);
    } catch (err) {
      setMessage('Camera access failed. Use manual member ID input.');
      stopScan();
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Attendance Scanner</h1>
      <p className="text-sm text-gray-600">
        Scan member QR code or type member ID, then attendance is posted to <code>/api/attendance/mark</code>.
      </p>

      <div className="bg-white rounded-xl border border-gray-200 p-4 space-y-3">
        <div className="flex gap-2">
          <input
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="Enter member record ID"
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <button
            onClick={() => markAttendance(code.trim())}
            className="px-4 py-2 rounded-lg bg-[#C21124] text-white hover:bg-[#a40f1f]"
          >
            Mark
          </button>
        </div>

        {supportsBarcodeDetector ? (
          <div className="space-y-2">
            <video ref={videoRef} className="w-full max-h-72 rounded-lg bg-black" muted playsInline />
            <div className="flex gap-2">
              <button
                onClick={startScan}
                className="px-4 py-2 rounded-lg border border-[#C21124] text-[#C21124] hover:bg-[#fff1f3]"
              >
                Start Camera Scan
              </button>
              <button
                onClick={stopScan}
                className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
              >
                Stop
              </button>
            </div>
          </div>
        ) : (
          <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            Browser QR camera scanning is not supported here. Use manual entry.
          </p>
        )}
      </div>

      {message && <div className="text-sm rounded-lg bg-slate-50 border border-slate-200 px-3 py-2">{message}</div>}
    </div>
  );
};

export default AdminScan;
