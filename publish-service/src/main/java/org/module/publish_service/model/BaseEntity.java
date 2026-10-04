package org.module.publish_service.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/3/2026 7:25 PM
 */
@Getter
@Setter
@MappedSuperclass
public class BaseEntity {
    private LocalDateTime created_at;
    private String created_by;
    private LocalDateTime modified_at;
    private String modified_by;
}
