package org.module.publish_service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/28/2026 11:38 PM
 */
@Slf4j
@Component
public class GenerateUtil {
    public String generateNameWithDateTime(String name) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MdHmm");
        String formatTime = now.format(formatter);
        if (!StringUtils.hasLength(name) || name.isEmpty()) {
            log.info("Field name for generate is required!");
            return formatTime;
        }

        String extractName3Digit = (name.length() > 3 ? name.substring(0, 3) : name).toUpperCase();
        return "%s%s".formatted(extractName3Digit, formatTime);
    }
}
