package org.module.publish_service.response;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 5:44 PM
 */
public record ApiErrorResponse(
        HttpStatus status,
        String message,
        LocalDateTime timestamp,
        Map<String, Object> errors
) {

}
