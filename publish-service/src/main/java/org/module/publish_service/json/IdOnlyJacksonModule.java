package org.module.publish_service.json;

import jakarta.persistence.Entity;
import org.module.publish_service.util.EntityGraphUtils;
import org.springframework.stereotype.Component;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/8/2026 10:55 PM
 */
@Component
public class IdOnlyJacksonModule extends SimpleModule {
    public IdOnlyJacksonModule() {
        super("IdOnlyJacksonModule");
        setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> beanProperties) {
                if (!beanDesc.getBeanClass().isAnnotationPresent(Entity.class)) {
                    return beanProperties;
                }
                return beanProperties.stream().<BeanPropertyWriter>map(IdOnlyPropertyWriter::new).toList();
            }
        });
    }

    private static class IdOnlyPropertyWriter extends BeanPropertyWriter {

        IdOnlyPropertyWriter(BeanPropertyWriter base) {
            super(base);
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctxt) throws Exception {
            String idName = EntityGraphUtils.idOnlyProperty(bean);
            if (idName != null && !idName.equals(getName())) {
                return;
            }
            super.serializeAsProperty(bean, gen, ctxt);
        }
    }
}
