const LEGACY_CORPS_NAMES = new Set([
  'kambuzuma',
  'high field temple',
  'high field templet',
  'hig field temple',
  'hig field templet',
]);

export const CANONICAL_CORPS_NAME = 'Highfield Temple';

export function getCorpsDisplayName(corpsName) {
  if (!corpsName || !String(corpsName).trim()) return null;
  const normalized = String(corpsName).trim().toLowerCase().replace(/\s+/g, ' ');
  if (LEGACY_CORPS_NAMES.has(normalized)
      || normalized.startsWith('high field tem')
      || normalized.startsWith('hig field tem')) {
    return CANONICAL_CORPS_NAME;
  }
  return String(corpsName).trim();
}
