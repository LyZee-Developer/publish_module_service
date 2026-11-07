package org.module.publish_service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

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

    public static String generateRandomCode(String prefix, Integer length) {
        String characters = "0123456789";
        length = Optional.ofNullable(length).orElse(5);
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            // Pick a random index from the character pool
            int index = random.nextInt(characters.length());
            // Append the character to our builder
            sb.append(characters.charAt(index));
        }

        String extractName3Digit = (prefix.length() > 3 ? prefix.substring(0, 3) : prefix).toUpperCase();
        return "%s%s".formatted(extractName3Digit, sb.toString());
    }
}
