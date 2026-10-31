package org.module.publish_service.util;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/30/2026 7:09 PM
 */

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Generated;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

public class BeanCloneUtils {

    @Generated
    private static final Logger log = LoggerFactory.getLogger(BeanCloneUtils.class);

    public BeanCloneUtils() {
    }

    private static void copyAllProperties(Object source, Object target, @Nullable Set<String> onlyFieldNames) {
        if (!ObjectUtils.isEmpty(source) && !ObjectUtils.isEmpty(target)) {
            if (source instanceof HibernateProxy) {
                HibernateProxy proxy = (HibernateProxy) source;
                LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
                if (!lazyInitializer.isUninitialized()) {
                    source = lazyInitializer.getImplementation();
                }
            }

            Class<?> sourceClass = source.getClass();
            Class<?> targetClass = target.getClass();
            if (onlyFieldNames == null) {
                onlyFieldNames = Set.of();
            }
            List<Field> targetFieldList = new ArrayList();

            for (Class<?> currClass = targetClass; currClass != null; currClass = currClass.getSuperclass()) {
                Field[] targetCurrFields = currClass.getDeclaredFields();
                targetFieldList.addAll(List.of(targetCurrFields));
            }

            if (!CollectionUtils.isEmpty(onlyFieldNames)) {
                Set<String> finalOnlyFieldNames = onlyFieldNames;
                targetFieldList = targetFieldList.stream().filter((f) -> finalOnlyFieldNames.contains(f.getName())).toList();
            }

            for (Field field : targetFieldList) {
                String fieldName = field.getName();

                try {
                    Field sourceField = getField(sourceClass, fieldName);
                    if (sourceField != null) {
                        sourceField.setAccessible(true);
                        field.setAccessible(true);
                        field.set(target, sourceField.get(source));
                    }

                } catch (IllegalAccessException ex) {
                    log.warn(ex.getMessage());
                }
            }
        }
    }


    private static Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException var3) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static void copyAllProperties(Object source, Object target) {
        copyAllProperties(source, target, (Set) null);
    }

    public static void copyAllIncludeProperties(Object source, Object target) {
        if (!ObjectUtils.isEmpty(source) && !ObjectUtils.isEmpty(target)) {
            Class<?> targetClass = target.getClass();
            JsonIncludeProperties jsonIncludeProperties = (JsonIncludeProperties) targetClass.getAnnotation(JsonIncludeProperties.class);
            if (ObjectUtils.isEmpty(jsonIncludeProperties)) {
                copyAllProperties(source, target);
            } else {
                String[] values = jsonIncludeProperties.value();
                copyAllProperties(source, target, Set.of(values));
            }
        }
    }
}
