package org.module.publish_service.view;

import org.module.publish_service.util.BeanCloneUtils;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/30/2026 7:08 PM
 */
public interface BaseView {
    default void copyAllIncludeProperties(Object entity) {
        BeanCloneUtils.copyAllIncludeProperties(entity, this);
    }

    default void copyAllProperties(Object entity) {
        BeanCloneUtils.copyAllProperties(entity, this);
    }
}
