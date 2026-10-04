package org.module.publish_service.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/3/2026 7:35 PM
 */
@Getter
@Setter
@MappedSuperclass
public class BaseActivateEntity extends BaseEntity{
    private Boolean isActivate;
}
