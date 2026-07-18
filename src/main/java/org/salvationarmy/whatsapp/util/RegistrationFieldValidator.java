package org.salvationarmy.whatsapp.util;

import java.util.Locale;
import java.util.Set;

/**
 * Validates required registration fields — rejects empty, placeholder, and sentinel values.
 */
public final class RegistrationFieldValidator {

    private static final Set<String> INVALID_VALUES = Set.of(
            "n/a", "na", "none", "null", "status", "skip", "unknown",
            "tbd", "pending", "placeholder", "-", "--", "..."
    );

    private RegistrationFieldValidator() {
    }

    public static boolean isMissingRequiredField(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return INVALID_VALUES.contains(trimmed.toLowerCase(Locale.ROOT));
    }

    public static String sanitizeRequiredField(String value) {
        return isMissingRequiredField(value) ? null : value.trim();
    }
}
