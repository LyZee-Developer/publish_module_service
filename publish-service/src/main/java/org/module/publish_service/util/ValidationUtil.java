package org.module.publish_service.util;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/8/2026 10:39 PM
 */

import org.module.publish_service.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * Programmatic checks for service-layer logic.
 * For request DTOs prefer Bean Validation annotations (@NotBlank, @Email, @Pattern).
 */
public final class ValidationUtil {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{8,15}$");

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    public static void requireNotBlank(String value, String field) {
        if (StringUtil.isBlank(value)) {
            throw new BusinessException("VALIDATION_ERROR", field + " is required");
        }
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new BusinessException("VALIDATION_ERROR", field + " is required");
        }
        return value;
    }

    public static void requireValidEmail(String email) {
        if (!isValidEmail(email)) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid email format");
        }
    }

    public static void requireValidPhone(String phone, String field) {
        if (!isValidPhone(phone)) {
            throw new BusinessException("VALIDATION_ERROR", "Invalid phone format for " + field);
        }
    }
}
