package org.module.publish_service.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/29/2026 6:17 PM
 */
@Component
public class EnumUtil {
    public <T extends Enum<T>> T validate(Class<T> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            String allowedValues = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            String theValue = value.isEmpty() ? "Unknow" : value;
            throw new IllegalArgumentException("Invalid " + theValue + ". Allowed values: (" + allowedValues + ")");
        }
    }
}
