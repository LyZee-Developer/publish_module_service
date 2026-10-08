package org.module.publish_service.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/8/2026 8:35 PM
 */
public final class StringUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    // Ambiguous characters (0/O, 1/l/I) removed for human-readable codes
    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private StringUtil() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Trim + lowercase, so "John@Mail.com " and "john@mail.com" are treated as the same unique email.
     */
    public static String normalizeEmail(String email) {
        String t = trimToNull(email);
        return t == null ? null : t.toLowerCase(Locale.ROOT);
    }

    /**
     * Keep only digits and a leading '+', e.g. "012 345-678" -> "012345678".
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.trim().replaceAll("(?!^\\+)[^0-9]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * "john.doe@example.com" -> "jo***@example.com". Use in logs and list views.
     */
    public static String maskEmail(String email) {
        if (isBlank(email)) return email;
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    /**
     * "012345678" -> "012***678".
     */
    public static String maskPhone(String phone) {
        if (isBlank(phone) || phone.length() < 7) return "****";
        int n = phone.length();
        return phone.substring(0, 3) + "*".repeat(n - 6) + phone.substring(n - 3);
    }

    /**
     * Cryptographically secure random code (OTP references, invite codes, etc.).
     */
    public static String randomAlphanumeric(int length) {
        if (length <= 0) throw new IllegalArgumentException("length must be > 0");
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
