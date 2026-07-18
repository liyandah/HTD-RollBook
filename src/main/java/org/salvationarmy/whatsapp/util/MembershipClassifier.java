package org.salvationarmy.whatsapp.util;

/**
 * Assigns fellowship {@code department} and {@code brigade_eligibility} from age, gender, marital status, and children count.
 * Order follows the agreed CASE rules (junior → senior → Home League / Men's → Youth → general).
 */
public final class MembershipClassifier {

    private MembershipClassifier() {
    }

    public static String normalizeGender(String raw) {
        if (raw == null) {
            return "";
        }
        String g = raw.trim().toUpperCase();
        if (g.isEmpty()) {
            return "";
        }
        if ("F".equals(g) || "FEMALE".equals(g)) {
            return "FEMALE";
        }
        if ("M".equals(g) || "MALE".equals(g) || "MAN".equals(g)) {
            return "MALE";
        }
        return g;
    }

    public static String normalizeMarital(String raw) {
        if (raw == null) {
            return "";
        }
        String m = raw.trim().toUpperCase().replace(' ', '_');
        if (m.isEmpty()) {
            return "";
        }
        if (m.startsWith("WIDOW")) {
            return "WIDOW";
        }
        if (m.contains("SINGLE") || "UNMARRIED".equals(m)) {
            return "SINGLE";
        }
        if (m.contains("MARRIED") && !m.contains("SINGLE")) {
            return "MARRIED";
        }
        if (m.contains("DIVORC")) {
            return "DIVORCED";
        }
        return m;
    }

    public static String classifyDepartment(int age, String genderNorm, String maritalNorm, int kids) {
        if (age <= 6) {
            return "Cradle Roll";
        }
        if (age <= 14) {
            return "Junior Soldier";
        }
        if (age > 60) {
            return "Senior Member (60+)";
        }

        boolean female = "FEMALE".equals(genderNorm);
        boolean male = "MALE".equals(genderNorm);
        boolean married = "MARRIED".equals(maritalNorm) || "WIDOW".equals(maritalNorm) || "DIVORCED".equals(maritalNorm);

        if (age <= 35 && !married && kids == 0) {
            if (female) {
                return "Youth (Young Women)";
            }
            if (male) {
                return "Youth (Young Men)";
            }
            return "Youth";
        }

        if (female) {
            return "Home League";
        }
        if (male) {
            return "Men's Fellowship";
        }
        return "Senior Soldier";
    }

    public static String classifyBrigadeEligibility(int age) {
        if (age >= 5 && age <= 12) {
            return "Junior Brigade Eligible";
        }
        if (age >= 13 && age <= 18) {
            return "Senior Brigade Eligible";
        }
        return "N/A";
    }
}
