package org.salvationarmy.whatsapp.util;

import java.util.regex.Pattern;

/**
 * Utility class for normalizing Zimbabwe phone numbers
 */
public class PhoneNumberUtil {
    
    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");
    
    /**
     * Normalizes a Zimbabwe phone number to international format (+263...)
     * 
     * Accepts:
     * - 0774914287 → +263774914287
     * - 774914287 → +263774914287
     * - +263774914287 → +263774914287
     * 
     * @param input Raw phone number input
     * @return Normalized phone number in format +263XXXXXXXXX, or null if invalid
     */
    public static String normalizeZimPhone(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        // Remove spaces, dashes, and other non-digit characters except +
        String cleaned = input.trim().replaceAll("[\\s\\-\\(\\)]", "");
        
        // Extract only digits (and + if present)
        if (!cleaned.matches("[\\+]?\\d+")) {
            return null;
        }
        
        // Remove + if present for processing
        boolean hasPlus = cleaned.startsWith("+");
        String digits = hasPlus ? cleaned.substring(1) : cleaned;
        
        // Validate: should only contain digits now
        if (!DIGITS_ONLY.matcher(digits).matches()) {
            return null;
        }
        
        int length = digits.length();
        
        // Case 1: Starts with 0 and length is 10 → replace 0 with +263
        if (digits.startsWith("0") && length == 10) {
            return "+263" + digits.substring(1);
        }
        
        // Case 2: Starts with 263 and length is 12 → add +
        if (digits.startsWith("263") && length == 12) {
            return "+" + digits;
        }
        
        // Case 3: Starts with +263 and length is 13 → already correct
        if (hasPlus && digits.startsWith("263") && length == 12) {
            return "+" + digits;
        }
        
        // Case 4: 9 digits starting with 7 (without leading 0) → add +263
        if (length == 9 && digits.startsWith("7")) {
            return "+263" + digits;
        }
        
        // Invalid format
        return null;
    }
    
    /**
     * Validates if a normalized phone number is valid
     * @param normalizedPhone Normalized phone number (should start with +263)
     * @return true if valid, false otherwise
     */
    public static boolean isValidNormalized(String normalizedPhone) {
        if (normalizedPhone == null) {
            return false;
        }
        
        // Should be +263 followed by 9 digits
        return normalizedPhone.matches("\\+263[0-9]{9}");
    }
}
