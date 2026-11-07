package org.module.publish_service.exception;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 11/1/2026 8:33 AM
 */
public class ApiResponseHandler extends RuntimeException {
    public ApiResponseHandler(String message) {
        super(message);
    }

    public ApiResponseHandler(String message, Throwable cause) {
        super(message, cause);
    }
}
