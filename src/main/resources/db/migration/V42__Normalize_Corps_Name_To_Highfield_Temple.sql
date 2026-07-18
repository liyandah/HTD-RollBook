-- Normalize legacy Corps names to the canonical display value.
UPDATE soldier_records
SET corps_name = 'Highfield Temple'
WHERE corps_name IS NULL
   OR TRIM(corps_name) = ''
   OR LOWER(TRIM(corps_name)) IN (
       'kambuzuma',
       'high field temple',
       'high field templet',
       'hig field temple',
       'hig field templet'
   )
   OR LOWER(TRIM(corps_name)) LIKE 'high field tem%'
   OR LOWER(TRIM(corps_name)) LIKE 'hig field tem%';

UPDATE soldier_records
SET enrolled_corps_name = 'Highfield Temple'
WHERE enrolled_corps_name IS NULL
   OR TRIM(enrolled_corps_name) = ''
   OR LOWER(TRIM(enrolled_corps_name)) IN (
       'kambuzuma',
       'high field temple',
       'high field templet',
       'hig field temple',
       'hig field templet'
   )
   OR LOWER(TRIM(enrolled_corps_name)) LIKE 'high field tem%'
   OR LOWER(TRIM(enrolled_corps_name)) LIKE 'hig field tem%';

UPDATE chat_session
SET corps_name = 'Highfield Temple'
WHERE corps_name IS NULL
   OR TRIM(corps_name) = ''
   OR LOWER(TRIM(corps_name)) IN (
       'kambuzuma',
       'high field temple',
       'high field templet',
       'hig field temple',
       'hig field templet'
   )
   OR LOWER(TRIM(corps_name)) LIKE 'high field tem%'
   OR LOWER(TRIM(corps_name)) LIKE 'hig field tem%';

UPDATE soldier_registration
SET corps_name = 'Highfield Temple'
WHERE corps_name IS NULL
   OR TRIM(corps_name) = ''
   OR LOWER(TRIM(corps_name)) IN (
       'kambuzuma',
       'high field temple',
       'high field templet',
       'hig field temple',
       'hig field templet'
   )
   OR LOWER(TRIM(corps_name)) LIKE 'high field tem%'
   OR LOWER(TRIM(corps_name)) LIKE 'hig field tem%';
