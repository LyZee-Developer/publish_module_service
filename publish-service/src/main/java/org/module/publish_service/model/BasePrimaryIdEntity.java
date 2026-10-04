package org.module.publish_service.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/3/2026 7:36 PM
 */
@Setter
@Getter
@MappedSuperclass
public class BasePrimaryIdEntity extends BaseActivateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
