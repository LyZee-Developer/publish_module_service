package org.module.publish_service.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/29/2026 9:21 PM
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class ApiResponse {
    private HttpStatus status;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String message;
    private Boolean isSuccess;
    private Object data;

    @JsonIgnore
    private final String messageDefault = "successfully";

    public ApiResponse success(String message) {
        return this.setValue(null, message, Boolean.TRUE, HttpStatus.OK);
    }

    public <T> ApiResponse success(T data) {
        return this.setValue(data, messageDefault, Boolean.TRUE, HttpStatus.OK);
    }

    public ApiResponse success(String message, HttpStatus status) {
        return this.setValue(null, message, Boolean.TRUE, status);
    }

    public ApiResponse error(String message, HttpStatus status) {
        return this.setValue(null, message, Boolean.FALSE, status);
    }

    public ApiResponse error(String message) {
        return this.setValue(null, message, Boolean.FALSE, HttpStatus.BAD_REQUEST);
    }

    private ApiResponse setValue(Object data, String message, Boolean isSuccess, HttpStatus status) {
        this.message = message;
        this.isSuccess = isSuccess;
        this.data = data;
        this.status = status;
        return this;
    }
}
