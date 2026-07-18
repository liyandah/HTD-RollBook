import React, { useEffect, useMemo, useRef, useState } from 'react';
import confetti from 'canvas-confetti';
import { CheckCircle2, Send, Smile } from 'lucide-react';
import html2canvas from 'html2canvas';
import http from '../../api/apiClient';
import DigitalIDCard from './DigitalIDCard';
import ImageAttachMenu from '../common/ImageAttachMenu';
import ImagePickButtons from '../common/ImagePickButtons';

const SESSION_KEY = 'public_bot_session_id';

const createSessionId = () => `guest-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

const quickRepliesByState = {
  ASK_GENDER: ['Male', 'Female'],
  ASK_DEPARTMENT: ['Youth', "Men's Fellowship", 'Home League'],
  ASK_FAMILY_SAME_SURNAME: ['Yes', 'No'],
  ASK_FAMILY_SAME_ADDRESS: ['Yes', 'No'],
  ASK_MARITAL_STATUS: ['Single', 'Married', 'Widowed'],
  ASK_FAMILY_RELATION_TYPE: ['Family', 'Friend'],
};

const Chatbot = () => {
  const sessionId = useMemo(() => {
    const existing = localStorage.getItem(SESSION_KEY);
    if (existing) return existing;
    const created = createSessionId();
    localStorage.setItem(SESSION_KEY, created);
    return created;
  }, []);

  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [state, setState] = useState('START');
  const [messages, setMessages] = useState([
    { role: 'bot', text: 'Welcome to HT-E Roll Book Bot. Type REGISTER to begin.' },
  ]);
  const [memberName, setMemberName] = useState(localStorage.getItem('public_bot_member_name') || '');
  const [recordCode, setRecordCode] = useState(localStorage.getItem('public_bot_record_code') || '');
  const [isSubmitted, setIsSubmitted] = useState(localStorage.getItem('public_bot_is_submitted') === 'true');
  const [hasCelebrated, setHasCelebrated] = useState(localStorage.getItem('public_bot_has_celebrated') === 'true');
  const [isVerifiedMember, setIsVerifiedMember] = useState(localStorage.getItem('public_bot_verified_member') === 'true');
  const [memberStatus, setMemberStatus] = useState(localStorage.getItem('public_bot_member_status') || '');
  const [hasShownMemberMenu, setHasShownMemberMenu] = useState(false);
  const [memberDepartment, setMemberDepartment] = useState(localStorage.getItem('public_bot_department') || '');
  const [personPhotoPath, setPersonPhotoPath] = useState(localStorage.getItem('public_bot_person_photo_path') || '');
  const digitalCardRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const messagesEndRef = useRef(null);
  const prevMessageCountRef = useRef(1);

  const isApproved = isVerifiedMember || memberStatus === 'APPROVED' || memberStatus === 'VERIFIED';
  const isPortalMember = ['APPROVED', 'VERIFIED', 'PENDING', 'IN_PROGRESS', 'REUPLOAD_REQUIRED'].includes(memberStatus);
  const isMenuTrigger = (text) => ['register', 'hi', 'hello', 'start'].includes((text || '').trim().toLowerCase());
  const personPhotoUrl = useMemo(() => {
    if (!personPhotoPath) return null;
    return personPhotoPath.startsWith('/api/images/') ? personPhotoPath : `/api/images/${personPhotoPath}`;
  }, [personPhotoPath]);

  const postBotMessage = async (text) => {
    setSending(true);
    try {
      const response = await http.post('/api/bot/message', { sessionId, message: text });
      const reply = response?.data?.replyText || 'Please try again.';
      setState(response?.data?.state || state);
      if (response?.data?.memberStatus) {
        setMemberStatus(response.data.memberStatus);
        localStorage.setItem('public_bot_member_status', response.data.memberStatus);
      }
      if (response?.data?.memberFirstName || response?.data?.memberLastName) {
        const fullName = `${response?.data?.memberFirstName || ''} ${response?.data?.memberLastName || ''}`.trim();
        if (fullName) {
          setMemberName(fullName);
          localStorage.setItem('public_bot_member_name', fullName);
        }
      }
      if (response?.data?.memberRecordCode) {
        setRecordCode(response.data.memberRecordCode);
        localStorage.setItem('public_bot_record_code', response.data.memberRecordCode);
      }
      if (response?.data?.memberDepartment) {
        setMemberDepartment(response.data.memberDepartment);
        localStorage.setItem('public_bot_department', response.data.memberDepartment);
      }
      if (response?.data?.personImagePath) {
        setPersonPhotoPath(response.data.personImagePath);
        localStorage.setItem('public_bot_person_photo_path', response.data.personImagePath);
      }
      if (response?.data?.status === 'DECLINED') {
        setMessages((prev) => [
          ...prev,
          {
            role: 'bot',
            text: 'Your registration was not approved at this time.',
            status: 'DECLINED',
            declineReason: response?.data?.declineReason || null,
          },
        ]);
      } else if (response?.data?.choices?.length) {
        setMessages((prev) => [
          ...prev,
          {
            role: 'bot',
            text: reply,
            choices: response.data.choices,
          },
        ]);
      } else {
        setMessages((prev) => [...prev, { role: 'bot', text: reply }]);
      }
      const completed = response?.data?.state === 'COMPLETE';
      if (completed) {
        setIsSubmitted(true);
        localStorage.setItem('public_bot_is_submitted', 'true');
      } else if (isSubmitted) {
        setIsSubmitted(false);
        localStorage.removeItem('public_bot_is_submitted');
      }
      if (response?.data?.state === 'VERIFIED_NOTIFICATION') {
        setIsVerifiedMember(true);
        localStorage.setItem('public_bot_verified_member', 'true');
      }
      if (response?.data?.status === 'APPROVED' || response?.data?.memberStatus === 'APPROVED' || response?.data?.memberStatus === 'VERIFIED') {
        setIsVerifiedMember(true);
        localStorage.setItem('public_bot_verified_member', 'true');
      }
      const codeMatch = reply.match(/Record ID:\s*([A-Z0-9-]+)/i);
      if (codeMatch?.[1]) {
        setRecordCode(codeMatch[1]);
        localStorage.setItem('public_bot_record_code', codeMatch[1]);
      }
    } catch (err) {
      setMessages((prev) => [...prev, { role: 'bot', text: 'Chat service is temporarily unavailable. Please try again.' }]);
    } finally {
      setSending(false);
    }
  };

  const sendMessage = async (e) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || sending) return;
    setMessages((prev) => [...prev, { role: 'user', text }]);
    if (isSubmitted && state === 'COMPLETE' && !isPortalMember && !isMenuTrigger(text)) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'bot',
          text: 'Your record is already being processed. You can check your status again in 24 hours.',
        },
      ]);
      setInput('');
      return;
    }
    if (state === 'ASK_FIRST_NAME') {
      localStorage.setItem('public_bot_first_name', text);
    }
    if (state === 'ASK_LAST_NAME') {
      const first = localStorage.getItem('public_bot_first_name') || '';
      const full = `${first} ${text}`.trim();
      setMemberName(full);
      localStorage.setItem('public_bot_member_name', full);
    }
    if (state === 'ASK_DEPARTMENT') {
      setMemberDepartment(text);
      localStorage.setItem('public_bot_department', text);
    }
    setInput('');
    await postBotMessage(text);
  };

  const handleQuickReply = async (text) => {
    if (sending) return;
    if (isSubmitted && state === 'COMPLETE' && !isPortalMember && !isMenuTrigger(text)) {
      setMessages((prev) => [
        ...prev,
        { role: 'user', text },
        { role: 'bot', text: 'Your record is already being processed. You can check your status again in 24 hours.' },
      ]);
      return;
    }
    setMessages((prev) => [...prev, { role: 'user', text }]);
    await postBotMessage(text);
  };

  const handleChoice = async (choice) => {
    if (sending) return;
    if (isApproved && state === 'COMPLETE') {
      setMessages((prev) => [...prev, { role: 'user', text: choice }]);
      if (choice === 'Register Family Member') {
        await startNewRegistration('family');
        return;
      }
      if (choice === 'Register a Friend') {
        await startNewRegistration('friend');
        return;
      }
      if (choice === 'Register Family/Friend') {
        await postBotMessage('Register Someone Else');
        return;
      }
      if (choice === 'View My Digital ID' || choice === 'Download Digital ID') {
        setMessages((prev) => [...prev, { role: 'bot', text: 'Your digital ID card is shown above. Use the Download Digital ID button to save it.' }]);
        await downloadDigitalId();
        return;
      }
      if (choice === 'Update Profile') {
        setMessages((prev) => [
          ...prev,
          {
            role: 'bot',
            text: 'Please tell me what profile detail you want to update, and I will guide you.',
          },
        ]);
        return;
      }
    }
    if (isSubmitted && state === 'COMPLETE' && !isPortalMember) {
      setMessages((prev) => [
        ...prev,
        { role: 'user', text: choice },
        { role: 'bot', text: 'Your record is already being processed. You can check your status again in 24 hours.' },
      ]);
      return;
    }
    setMessages((prev) => [...prev, { role: 'user', text: choice }]);
    await postBotMessage(choice);
  };

  const uploadImageFile = async (file) => {
    if (!file) return;
    if (state !== 'ASK_PERSON_IMAGE' && state !== 'ASK_CERT_IMAGE') {
      setMessages((prev) => [...prev, { role: 'bot', text: 'Image upload is not expected yet. Please continue the questions.' }]);
      return;
    }

    if (!file.type.startsWith('image/')) {
      setMessages((prev) => [...prev, { role: 'bot', text: 'Please select an image file (jpg, png, jpeg, etc.).' }]);
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setMessages((prev) => [...prev, { role: 'bot', text: 'Image size must be less than 5MB.' }]);
      return;
    }

    const formData = new FormData();
    formData.append('sessionId', sessionId);
    formData.append('file', file);
    formData.append('imageType', state === 'ASK_CERT_IMAGE' ? 'cert' : 'person');

    setSending(true);
    setMessages((prev) => [...prev, { role: 'user', text: `[Uploaded image: ${file.name}]` }]);
    try {
      const response = await http.post('/api/bot/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      const reply = response?.data?.replyText || 'Image uploaded.';
      const returnedPhotoPath = response?.data?.photoPath;
      setState(response?.data?.state || state);
      if (returnedPhotoPath && state === 'ASK_PERSON_IMAGE') {
        setPersonPhotoPath(returnedPhotoPath);
        localStorage.setItem('public_bot_person_photo_path', returnedPhotoPath);
      }
      if (returnedPhotoPath) {
        localStorage.setItem('public_bot_photo_path', returnedPhotoPath);
      }
      setMessages((prev) => [...prev, { role: 'bot', text: reply }]);
      if (response?.data?.state === 'COMPLETE') {
        setIsSubmitted(true);
        localStorage.setItem('public_bot_is_submitted', 'true');
      }
    } catch (error) {
      setMessages((prev) => [...prev, { role: 'bot', text: 'Failed to upload image. Please try again.' }]);
    } finally {
      setSending(false);
    }
  };

  const restartRegistration = async () => {
    if (sending) return;
    setIsSubmitted(false);
    localStorage.removeItem('public_bot_is_submitted');
    setHasCelebrated(false);
    localStorage.removeItem('public_bot_has_celebrated');
    setIsVerifiedMember(false);
    localStorage.removeItem('public_bot_verified_member');
    setMemberStatus('');
    localStorage.removeItem('public_bot_member_status');
    setHasShownMemberMenu(false);
    setMemberDepartment('');
    localStorage.removeItem('public_bot_department');
    setPersonPhotoPath('');
    localStorage.removeItem('public_bot_person_photo_path');
    setMessages((prev) => [...prev, { role: 'user', text: 'Fix & Re-submit' }]);
    await postBotMessage('reset');
  };

  const downloadDigitalId = async () => {
    if (!digitalCardRef.current) return;
    const canvas = await html2canvas(digitalCardRef.current, {
      backgroundColor: '#ffffff',
      scale: 2,
      useCORS: true,
    });
    const link = document.createElement('a');
    link.download = `${(memberName || 'member').replace(/\s+/g, '_').toLowerCase()}_digital_id.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  };

  const startNewRegistration = async (type) => {
    if (sending) return;
    setMessages((prev) => [...prev, { role: 'user', text: type === 'friend' ? 'Register a Friend' : 'Register a Family Member' }]);
    await postBotMessage('Register Someone Else');
    await postBotMessage(type === 'friend' ? 'Friend' : 'Family');
  };

  useEffect(() => {
    const last = messages[messages.length - 1];
    const countIncreased = messages.length > prevMessageCountRef.current;
    prevMessageCountRef.current = messages.length;
    if (countIncreased && last?.role === 'bot') {
      requestAnimationFrame(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
      });
    }
  }, [messages]);

  useEffect(() => {
    const hydrateMemberPortal = async () => {
      try {
        const res = await http.post('/api/bot/message', { sessionId, message: 'status' });
        const data = res?.data || {};
        if (data.state) {
          setState(data.state);
        }
        if (data.memberStatus) {
          setMemberStatus(data.memberStatus);
          localStorage.setItem('public_bot_member_status', data.memberStatus);
        }
        if (data.memberFirstName || data.memberLastName) {
          const fullName = `${data.memberFirstName || ''} ${data.memberLastName || ''}`.trim();
          if (fullName) {
            setMemberName(fullName);
            localStorage.setItem('public_bot_member_name', fullName);
          }
        }
        if (data.memberRecordCode) {
          setRecordCode(data.memberRecordCode);
          localStorage.setItem('public_bot_record_code', data.memberRecordCode);
        }
        if (data.memberDepartment) {
          setMemberDepartment(data.memberDepartment);
          localStorage.setItem('public_bot_department', data.memberDepartment);
        }
        if (data.personImagePath) {
          setPersonPhotoPath(data.personImagePath);
          localStorage.setItem('public_bot_person_photo_path', data.personImagePath);
        }
        if (data.memberStatus === 'APPROVED' || data.memberStatus === 'VERIFIED') {
          setIsVerifiedMember(true);
          localStorage.setItem('public_bot_verified_member', 'true');
        }
        if (data.state === 'ASK_CERT_IMAGE' && data.replyText) {
          setIsSubmitted(false);
          localStorage.removeItem('public_bot_is_submitted');
          setMessages((prev) => {
            const exists = prev.some((m) => m.role === 'bot' && m.text === data.replyText);
            if (exists) return prev;
            return [...prev, { role: 'bot', text: data.replyText }];
          });
        }
      } catch (e) {
        // Ignore hydration errors and continue with manual chat flow.
      }
    };
    hydrateMemberPortal();
  }, [sessionId]);

  useEffect(() => {
    const pollApprovalStatus = async () => {
      if (!isSubmitted || state !== 'COMPLETE' || sending) return;
      try {
        const statusResponse = await http.post('/api/bot/message', { sessionId, message: 'status' });
        const nextState = statusResponse?.data?.state;
        const reply = statusResponse?.data?.replyText || '';
        const responseStatus = statusResponse?.data?.status;
        if (statusResponse?.data?.memberStatus) {
          setMemberStatus(statusResponse.data.memberStatus);
          localStorage.setItem('public_bot_member_status', statusResponse.data.memberStatus);
        }
        if (responseStatus === 'APPROVED') {
          setIsVerifiedMember(true);
          localStorage.setItem('public_bot_verified_member', 'true');
        }
        if (statusResponse?.data?.memberStatus === 'APPROVED' || statusResponse?.data?.memberStatus === 'VERIFIED') {
          setIsVerifiedMember(true);
          localStorage.setItem('public_bot_verified_member', 'true');
        }
        if (nextState === 'VERIFIED_NOTIFICATION') {
          setState(nextState);
          setIsVerifiedMember(true);
          localStorage.setItem('public_bot_verified_member', 'true');

          if (!hasCelebrated) {
            confetti({
              particleCount: 150,
              spread: 70,
              origin: { y: 0.6 },
              colors: ['#C21124', '#ffffff', '#6F0A15'],
            });
            setHasCelebrated(true);
            localStorage.setItem('public_bot_has_celebrated', 'true');
          }

          setMessages((prev) => {
            const alreadyShown = prev.some((m) => m.role === 'bot' && m.text === reply);
            if (alreadyShown) return prev;
            return [
              ...prev.filter((m) => !(m.role === 'bot' && /pending|review/i.test(m.text || ''))),
              { role: 'bot', text: '✅ Your registration has been APPROVED! Welcome to the official Roll Book.', type: 'success' },
              { role: 'bot', text: reply },
            ];
          });

          const continueResponse = await http.post('/api/bot/message', { sessionId, message: 'continue' });
          const continueText = continueResponse?.data?.replyText;
          const continueState = continueResponse?.data?.state;
          if (continueState) setState(continueState);
          if (continueText) {
            setMessages((prev) => [...prev, { role: 'bot', text: continueText }]);
          }
        } else if (nextState === 'ASK_CERT_IMAGE') {
          setState(nextState);
          setIsSubmitted(false);
          localStorage.removeItem('public_bot_is_submitted');
          setMessages((prev) => {
            const alreadyShown = prev.some((m) => m.role === 'bot' && m.text === reply);
            if (alreadyShown) return prev;
            return [
              ...prev.filter((m) => !(m.role === 'bot' && /pending|verification in progress/i.test(m.text || ''))),
              { role: 'bot', text: reply || 'The Admin has requested a clearer certificate image. Please upload it now.' },
            ];
          });
        }
      } catch (err) {
        // Keep polling quietly; user can still continue manually.
      }
    };

    const interval = window.setInterval(pollApprovalStatus, 10000);
    return () => window.clearInterval(interval);
  }, [hasCelebrated, isSubmitted, sending, sessionId, state]);

  useEffect(() => {
    if (!isPortalMember || state !== 'COMPLETE' || hasShownMemberMenu) return;
    const displayName = memberName || 'Member';
    const portalText = isApproved
      ? `Welcome back, ${displayName}! You are a verified member of Highfield Temple. How can I help you today?`
      : `Welcome back, ${displayName}! Your profile is in progress. You can continue with one of these actions:`;
    setMessages((prev) => [
      ...prev,
      {
        role: 'bot',
        text: portalText,
        choices: ['Register Family Member', 'Register a Friend', 'Download Digital ID'],
      },
    ]);
    setHasShownMemberMenu(true);
  }, [hasShownMemberMenu, isApproved, isPortalMember, memberName, state]);

  return (
    <div className="min-h-screen bg-[#f4f5f7] p-4 sm:p-6 flex items-center justify-center">
      <div className="w-full max-w-md bg-white rounded-xl shadow-xl overflow-hidden">
        <div className="bg-gradient-to-r from-[#C21124] to-[#6F0A15] px-4 py-4 text-white">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-white overflow-hidden flex items-center justify-center">
              <img src="/shield.jpg?v=20260414" alt="Salvation Army Shield" className="w-full h-full object-cover" />
            </div>
            <div>
              <h2 className="font-bold text-lg">HT-E ChatBot</h2>
              <p className="text-xs text-white/80">Salvation Army HT-E Roll Book Registration Bot.</p>
              {isApproved && (
                <div className="mt-1 inline-flex items-center rounded-full bg-white/20 border border-white/30 px-2 py-0.5 text-[11px] font-semibold text-white">
                  <CheckCircle2 className="w-3 h-3 mr-1" />
                  Verified Member
                </div>
              )}
              <p className="text-[11px] text-white/80 mt-1">
                Name: {memberName || 'Not captured yet'} | {isApproved ? 'Status: Approved' : `Account: ${recordCode || sessionId}`}
              </p>
            </div>
          </div>
        </div>

        <div ref={messagesContainerRef} className="h-[62vh] overflow-y-auto p-4 space-y-3 bg-[#f8f9fb]">
          {isApproved && (
            <div className="space-y-3">
              <DigitalIDCard
                ref={digitalCardRef}
                memberName={memberName}
                memberId={recordCode || sessionId}
                memberType={memberDepartment || 'Registered Member'}
                photoUrl={personPhotoUrl}
              />
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <button
                  onClick={() => startNewRegistration('family')}
                  className="bg-[#C21124] text-white py-2 px-3 rounded-xl text-sm font-semibold hover:bg-[#a00e1e]"
                >
                  Register a Family Member
                </button>
                <button
                  onClick={() => startNewRegistration('friend')}
                  className="bg-[#C21124] text-white py-2 px-3 rounded-xl text-sm font-semibold hover:bg-[#a00e1e]"
                >
                  Register a Friend
                </button>
                <button
                  onClick={downloadDigitalId}
                  className="bg-green-600 text-white py-2 px-3 rounded-xl text-sm font-semibold hover:bg-green-700"
                >
                  Download Digital ID
                </button>
              </div>
            </div>
          )}
          {messages.map((m, idx) => (
            <div key={`${m.role}-${idx}`} className={m.role === 'user' ? 'flex justify-end' : 'flex justify-start'}>
              {m.role === 'bot' && m.status === 'DECLINED' ? (
                <div className="max-w-[90%] space-y-2">
                  <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-xl text-red-700 text-sm">
                    <p className="font-bold">Registration Declined</p>
                    <p>{m.declineReason || 'No specific reason provided. Please contact the Corps Secretary.'}</p>
                  </div>
                  <button
                    onClick={restartRegistration}
                    className="bg-[#C21124] text-white py-2 px-4 rounded-full text-sm font-bold shadow-md hover:bg-[#a00e1e]"
                  >
                    Fix & Re-submit
                  </button>
                </div>
              ) : m.role === 'bot' && m.choices?.length ? (
                <div className="max-w-[90%] space-y-2">
                  <div className="relative rounded-2xl rounded-bl-md bg-[#C21124] text-white px-3 py-2 text-sm whitespace-pre-wrap">
                    {m.text}
                    <span className="absolute -left-1 bottom-2 w-2 h-2 bg-[#C21124] rotate-45" />
                  </div>
                  <div className="flex flex-col gap-2">
                    {m.choices.map((choice) => (
                      <button
                        key={`${idx}-${choice}`}
                        onClick={() => handleChoice(choice)}
                        className="bg-[#C21124] text-white py-2 px-4 rounded-2xl text-sm font-bold shadow-md hover:bg-[#a00e1e]"
                      >
                        {choice}
                      </button>
                    ))}
                  </div>
                </div>
              ) : m.role === 'bot' && m.type === 'success' ? (
                <div className="relative max-w-[85%] rounded-2xl rounded-bl-md bg-green-600 text-white px-3 py-2 text-sm whitespace-pre-wrap">
                  {m.text}
                  <span className="absolute -left-1 bottom-2 w-2 h-2 bg-green-600 rotate-45" />
                </div>
              ) : m.role === 'bot' ? (
                <div className="relative max-w-[85%] rounded-2xl rounded-bl-md bg-[#C21124] text-white px-3 py-2 text-sm whitespace-pre-wrap">
                  {m.text}
                  <span className="absolute -left-1 bottom-2 w-2 h-2 bg-[#C21124] rotate-45" />
                </div>
              ) : (
                <div className="max-w-[85%] rounded-2xl rounded-br-md bg-[#E9EDF0] text-gray-900 px-3 py-2 text-sm whitespace-pre-wrap">
                  {m.text}
                </div>
              )}
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {quickRepliesByState[state] && (
          <div className="px-4 py-2 bg-white border-t border-gray-100 flex flex-wrap gap-2">
            {quickRepliesByState[state].map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => handleQuickReply(option)}
                className="px-3 py-1.5 rounded-full border border-[#C21124]/30 text-[#7b1020] text-sm hover:bg-[#fff1f3]"
              >
                {option}
              </button>
            ))}
          </div>
        )}

        {(state === 'ASK_PERSON_IMAGE' || state === 'ASK_CERT_IMAGE') && (
          <div className="px-4 py-3 bg-white border-t border-gray-100 space-y-2">
            <p className="text-xs text-gray-500">
              {state === 'ASK_PERSON_IMAGE'
                ? 'Please add your personal photo:'
                : 'Please upload your certificate image:'}
            </p>
            <ImagePickButtons onFileSelected={uploadImageFile} disabled={sending} compact />
          </div>
        )}

        <form onSubmit={sendMessage} className="bg-white border-t border-gray-100 px-3 py-3 flex items-center gap-2">
          <button type="button" className="text-gray-500 hover:text-gray-700 min-w-[44px] min-h-[44px] flex items-center justify-center touch-manipulation" aria-label="Emoji">
            <Smile size={18} />
          </button>
          <ImageAttachMenu
            onFileSelected={uploadImageFile}
            disabled={sending}
            iconClassName="w-[18px] h-[18px]"
            title="Attach image"
          />
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type your message..."
            className="flex-1 rounded-full border border-gray-200 px-4 py-2 text-sm text-gray-900 outline-none focus:border-[#C21124]"
          />
          <button
            type="submit"
            disabled={sending}
            className="w-9 h-9 rounded-full bg-[#C21124] text-white flex items-center justify-center hover:bg-[#a40f1f] disabled:opacity-70"
            aria-label="Send"
          >
            <Send size={16} />
          </button>
        </form>
      </div>
    </div>
  );
};

export default Chatbot;
