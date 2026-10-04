package org.module.publish_service.exception;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 4:35 PM
 */

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
