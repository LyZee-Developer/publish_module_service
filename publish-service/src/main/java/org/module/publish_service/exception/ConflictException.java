package org.module.publish_service.exception;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/5/2026 9:45 PM
 */
public class ConflictException extends RuntimeException{
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictException() {
        super();
    }
}
