const INVALID_REQUIRED_VALUES = new Set([
  'n/a',
  'na',
  'none',
  'null',
  'status',
  'skip',
  'unknown',
  'tbd',
  'pending',
  'placeholder',
  '-',
  '--',
  '...',
]);

export function isMissingRequiredField(value) {
  if (value == null) return true;
  const trimmed = String(value).trim();
  if (!trimmed) return true;
  return INVALID_REQUIRED_VALUES.has(trimmed.toLowerCase());
}

export function sanitizeRequiredField(value) {
  return isMissingRequiredField(value) ? null : String(value).trim();
}

export const REQUIRED_REGISTRATION_FIELD_LABELS = {
  phoneNumber: 'Mobile Number',
  nextOfKinName: 'Next of Kin Name',
  nextOfKinPhone: 'Next of Kin Mobile Number',
  favoriteSong: 'Favourite Song',
  favoriteBibleVerse: 'Favourite Bible Verse',
};

export function getMissingRequiredRegistrationFields(record) {
  if (!record) return [];
  return Object.entries(REQUIRED_REGISTRATION_FIELD_LABELS)
    .filter(([field]) => isMissingRequiredField(record[field]))
    .map(([, label]) => label);
}
