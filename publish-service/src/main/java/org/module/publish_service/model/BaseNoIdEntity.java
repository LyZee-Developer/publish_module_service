package org.module.publish_service.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/3/2026 7:37 PM
 */
@MappedSuperclass
@Setter
@Getter
public class BaseNoIdEntity extends BaseActivateEntity{

}
