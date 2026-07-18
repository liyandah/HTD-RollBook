package org.salvationarmy.whatsapp.util;

import java.util.Locale;

public final class CorpsNameUtil {

    public static final String CANONICAL_CORPS_NAME = "Highfield Temple";

    private CorpsNameUtil() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return CANONICAL_CORPS_NAME;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (isLegacyCorpsName(normalized)) {
            return CANONICAL_CORPS_NAME;
        }
        return raw.trim();
    }

    public static boolean isLegacyCorpsName(String normalizedLower) {
        if (normalizedLower == null || normalizedLower.isBlank()) {
            return true;
        }
        return normalizedLower.equals("kambuzuma")
                || normalizedLower.equals("high field temple")
                || normalizedLower.equals("high field templet")
                || normalizedLower.equals("hig field temple")
                || normalizedLower.equals("hig field templet")
                || normalizedLower.startsWith("high field tem")
                || normalizedLower.startsWith("hig field tem");
    }
}
