/**
 * Reset and prepare a file input before the user opens the picker.
 * Uses label htmlFor (not programmatic .click()) so Android WebView/APK can open camera or gallery.
 */
export function prepareFileInput(input, mode = 'gallery') {
  if (!input) return;
  if (mode === 'camera') {
    input.setAttribute('capture', 'environment');
  } else {
    input.removeAttribute('capture');
  }
  input.value = '';
}
