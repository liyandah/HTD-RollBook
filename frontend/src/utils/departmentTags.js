/** CSS classes in index.css: dept-tag, tag-home-league, tag-mens-fellowship, etc. */

export function getDepartmentTagClass(dept) {
  if (!dept) return 'tag-default';
  if (dept === 'Home League') return 'tag-home-league';
  if (dept === "Men's Fellowship") return 'tag-mens-fellowship';
  if (dept === 'Youth') return 'tag-youth';
  if (dept === 'Junior Soldier') return 'tag-junior';
  if (dept === 'Senior Citizen (Old Age)') return 'tag-senior';
  if (dept === 'General Member') return 'tag-general';
  return 'tag-default';
}

export function getBrigadeEligibilityLabel(rec) {
  const raw = (rec.brigadeEligibility || '').trim();
  const age = rec.age;
  const isBlank = !raw || raw === 'N/A';

  if (age != null && age > 18) {
    if (isBlank) return 'Adult Member';
    return raw;
  }
  if (age != null && age <= 18 && isBlank) {
    return 'Not eligible';
  }
  if (isBlank) return 'Not assigned';
  return raw;
}

export function getBrigadeEligibilityTagClass(label) {
  if (label === 'Adult Member') return 'tag-adult-member';
  if (label.includes('Brigade Eligible')) return 'tag-brigade-eligible';
  return 'tag-default';
}
